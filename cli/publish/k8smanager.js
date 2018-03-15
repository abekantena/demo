"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const k8s = require('@kubernetes/typescript-node');
const btoa = require('btoa');
const chalk = require("chalk");
const fs = require("fs");
const path = require("path");
const jsyaml = require("js-yaml");
const deployui_1 = require("./deployui");
const MAX_RETRY = 36;
const DEFAULT_TIMEOUT = 10000;
class K8sManager {
    constructor(namespace, kubeConfigFilePath, config) {
        this._retryCount = 0;
        this._namespace = namespace;
        this._configFilePath = kubeConfigFilePath;
        this._config = config;
        this._api = k8s.Config.fromFile(this._configFilePath);
        const kc = new k8s.KubeConfig();
        kc.loadFromFile(kubeConfigFilePath);
        this._betaApi = new k8s.Extensions_v1beta1Api(kc.getCurrentCluster().server);
        this._betaApi.authentications.default = kc;
        this._secret = new k8s.V1Secret();
        this._secret.apiVersion = 'v1';
        this._secret.metadata = new k8s.V1ObjectMeta();
        this._secret.metadata.name = 'tls-certificate';
        this._secret.metadata.namespace = this._namespace;
        this._secret.kind = 'Secret';
        this._secret.type = 'Opaque';
        this._secret.data = {};
        this._deployUI = deployui_1.default.instance;
    }
    createNamespace(name) {
        const ns = new k8s.V1Namespace();
        ns.apiVersion = 'v1';
        ns.kind = 'Namespace';
        ns.metadata = {};
        ns.metadata.name = this._namespace;
        return new Promise((resolve, reject) => {
            const timer = setInterval(() => {
                return this._api.createNamespace(ns)
                    .then((result) => {
                    clearInterval(timer);
                    resolve(result);
                })
                    .catch((error) => {
                    if (error.code === 'ETIMEDOUT' && this._retryCount < MAX_RETRY) {
                        this._retryCount++;
                        console.log(`${chalk.yellow('Create namespace: retrying', this._retryCount.toString(), 'of', MAX_RETRY.toString())}`);
                    }
                    else {
                        let err = error;
                        if (error.code !== 'ETIMEDOUT') {
                            // Convert a response to properl format in case of json
                            err = JSON.stringify(error, null, 2);
                        }
                        clearInterval(timer);
                        reject(err);
                    }
                });
            }, DEFAULT_TIMEOUT);
        });
    }
    deleteAll() {
        return this.deleteSecrets()
            .then(() => {
            return this.deleteConfigMap();
        })
            .then(() => {
            return this.deleteDeployment();
        });
    }
    deleteSecrets() {
        return this._api.deleteNamespacedSecret(this._secret.metadata.name, this._namespace, this._secret);
    }
    deleteConfigMap() {
        const configPath = __dirname + path.sep + 'solutions/remotemonitoring/scripts/individual/deployment-configmap.yaml';
        const configMap = jsyaml.safeLoad(fs.readFileSync(configPath, 'UTF-8'));
        configMap.metadata.namespace = this._namespace;
        return this._api.deleteNamespacedConfigMap(configMap.metadata.name, this._namespace, configMap);
    }
    deleteDeployment() {
        const promises = new Array();
        const allInOnePath = __dirname + path.sep + 'solutions/remotemonitoring/scripts/all-in-one.yaml';
        const data = fs.readFileSync(allInOnePath, 'UTF-8');
        const allInOne = jsyaml.safeLoadAll(data, (doc) => {
            doc.metadata.namespace = this._namespace;
            switch (doc.kind) {
                case 'Service':
                    promises.push(this._api.deleteNamespacedService(doc.metadata.name, this._namespace, doc));
                    break;
                case 'ReplicationController':
                    promises.push(this._api.deleteNamespacedReplicationController(doc.metadata.name, this._namespace, doc));
                    break;
                case 'Deployment':
                    promises.push(this._betaApi.deleteNamespacedDeployment(doc.metadata.name, this._namespace, doc));
                    break;
                case 'Ingress':
                    doc.spec.rules[0].host = this._config.DNS;
                    doc.spec.tls[0].hosts[0] = this._config.DNS;
                    promises.push(this._betaApi.deleteNamespacedIngress(doc.metadata.name, this._namespace, doc));
                    break;
                default:
                    console.log('Unexpected kind found in yaml file');
            }
        });
        return Promise.all(promises);
    }
    setupAll() {
        this._deployUI.start('Setting up Kubernetes: Uploading secrets');
        return this.setupSecrets()
            .then(() => {
            this._deployUI.start('Setting up Kubernetes: Uploading config map');
            return this.setupConfigMap();
        })
            .then(() => {
            this._deployUI.start('Setting up Kubernetes: Starting web app and microservices');
            return this.setupDeployment();
        });
    }
    setupSecrets() {
        this._secret.data['tls.crt'] = btoa(this._config.TLS.cert);
        this._secret.data['tls.key'] = btoa(this._config.TLS.key);
        return new Promise((resolve, reject) => {
            const timer = setInterval(() => {
                return this._api.createNamespacedSecret(this._namespace, this._secret)
                    .then((result) => {
                    clearInterval(timer);
                    resolve(result);
                })
                    .catch((error) => {
                    if (error.code === 'ETIMEDOUT' && this._retryCount < MAX_RETRY) {
                        this._retryCount++;
                    }
                    else {
                        let err = error;
                        if (error.code !== 'ETIMEDOUT') {
                            // Convert a response to properl format in case of json
                            err = JSON.stringify(error, null, 2);
                        }
                        clearInterval(timer);
                        reject(err);
                    }
                });
            }, DEFAULT_TIMEOUT);
        });
    }
    setupConfigMap() {
        const configPath = __dirname + path.sep + 'solutions/remotemonitoring/scripts/individual/deployment-configmap.yaml';
        const configMap = jsyaml.safeLoad(fs.readFileSync(configPath, 'UTF-8'));
        configMap.metadata.namespace = this._namespace;
        configMap.data['security.auth.audience'] = this._config.ApplicationId;
        configMap.data['security.auth.issuer'] = 'https://sts.windows.net/' + this._config.AADTenantId + '/';
        configMap.data['security.application.secret'] = this.genPassword();
        configMap.data['bing.map.key'] = this._config.BingMapApiQueryKey ? this._config.BingMapApiQueryKey : '';
        configMap.data['iothub.connstring'] = this._config.IoTHubConnectionString;
        configMap.data['docdb.connstring'] = this._config.DocumentDBConnectionString;
        configMap.data['iothubreact.hub.name'] = this._config.EventHubName;
        configMap.data['iothubreact.hub.endpoint'] = this._config.EventHubEndpoint;
        configMap.data['iothubreact.hub.partitions'] = this._config.EventHubPartitions;
        configMap.data['iothubreact.access.connstring'] = this._config.IoTHubConnectionString;
        configMap.data['iothubreact.azureblob.account'] = this._config.AzureStorageAccountName;
        configMap.data['iothubreact.azureblob.key'] = this._config.AzureStorageAccountKey;
        configMap.data['iothubreact.azureblob.endpointsuffix'] = this._config.AzureStorageEndpointSuffix;
        let deploymentConfig = configMap.data['webui-config.js'];
        deploymentConfig = deploymentConfig.replace('{TenantId}', this._config.AADTenantId);
        deploymentConfig = deploymentConfig.replace('{ApplicationId}', this._config.ApplicationId);
        deploymentConfig = deploymentConfig.replace('{AADLoginInstance}', this._config.AADLoginURL);
        configMap.data['webui-config.js'] = deploymentConfig;
        return this._api.createNamespacedConfigMap(this._namespace, configMap);
    }
    setupDeployment() {
        const promises = new Array();
        const allInOnePath = __dirname + path.sep + 'solutions/remotemonitoring/scripts/all-in-one.yaml';
        const data = fs.readFileSync(allInOnePath, 'UTF-8');
        const allInOne = jsyaml.safeLoadAll(data, (doc) => {
            doc.metadata.namespace = this._namespace;
            switch (doc.kind) {
                case 'Service':
                    if (doc.spec.type === 'LoadBalancer') {
                        doc.spec.loadBalancerIP = this._config.LoadBalancerIP;
                    }
                    promises.push(this._api.createNamespacedService(this._namespace, doc));
                    break;
                case 'ReplicationController':
                    promises.push(this._api.createNamespacedReplicationController(this._namespace, doc));
                    break;
                case 'Deployment':
                    const imageName = doc.spec.template.spec.containers[0].image;
                    if (imageName.includes('{runtime}')) {
                        doc.spec.template.spec.containers[0].image = imageName.replace('{runtime}', this._config.Runtime);
                    }
                    promises.push(this._betaApi.createNamespacedDeployment(this._namespace, doc));
                    break;
                case 'Ingress':
                    doc.spec.rules[0].host = this._config.DNS;
                    doc.spec.tls[0].hosts[0] = this._config.DNS;
                    promises.push(this._betaApi.createNamespacedIngress(this._namespace, doc));
                    break;
                default:
                    console.log('Unexpected kind found in yaml file');
            }
        });
        return Promise.all(promises);
    }
    genPassword() {
        const chs = '0123456789-ABCDEVISFGHJKLMNOPQRTUWXYZ_abcdevisfghjklmnopqrtuwxyz'.split('');
        const len = chs.length;
        let result = '';
        for (let i = 0; i < 40; i++) {
            result += chs[Math.floor(len * Math.random())];
        }
        return result;
    }
}
exports.K8sManager = K8sManager;
//# sourceMappingURL=k8smanager.js.map
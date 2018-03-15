"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const chalk = require("chalk");
const fs = require("fs");
const os = require("os");
const path = require("path");
const fetch = require("node-fetch");
const azure_arm_resource_1 = require("azure-arm-resource");
const ms_rest_azure_1 = require("ms-rest-azure");
const deployui_1 = require("./deployui");
const ssh2_1 = require("ssh2");
const k8smanager_1 = require("./k8smanager");
const config_1 = require("./config");
const MAX_RETRY = 36;
const KUBEDIR = os.homedir() + path.sep + '.kube';
// We are using BingMap APIs with plan = internal1
// It only allows to have 2 apis per subscription
const MAX_BING_MAP_APIS_FOR_INTERNAL1_PLAN = 2;
class DeploymentManager {
    constructor(options, subscriptionId, solutionType, sku) {
        this._options = options;
        this._solutionType = solutionType;
        this._sku = sku;
        this._subscriptionId = subscriptionId;
        const baseUri = this._options.environment ? this._options.environment.resourceManagerEndpointUrl : undefined;
        this._client = new azure_arm_resource_1.ResourceManagementClient(new ms_rest_azure_1.DeviceTokenCredentials(this._options), subscriptionId, baseUri);
    }
    getLocations() {
        // Currently IotHub is not supported in all the regions so using it to get the available locations
        return this._client.providers.get('Microsoft.Devices')
            .then((providers) => {
            if (providers.resourceTypes) {
                const resourceType = providers.resourceTypes.filter((x) => x.resourceType && x.resourceType.toLowerCase() === 'iothubs');
                if (resourceType && resourceType.length) {
                    return resourceType[0].locations;
                }
            }
        });
    }
    submit(answers) {
        if (!!!answers || !!!answers.solutionName || !!!answers.subscriptionId || !!!answers.location) {
            return Promise.reject('Solution name, subscription id and location cannot be empty');
        }
        const location = answers.location;
        const deployment = {
            properties: {
                mode: 'Incremental',
            }
        };
        const deployUI = deployui_1.default.instance;
        const deploymentName = 'deployment-' + answers.solutionName;
        let deploymentProperties = null;
        let resourceGroupUrl;
        let freeBingMapResourceCount = 0;
        let resourceGroup = {
            location,
            // TODO: Explore if it makes sense to add more tags, e.g. Language(Java/.Net), version etc
            tags: { IotSolutionType: this._solutionType },
        };
        const environment = this._options.environment;
        let portalUrl = 'https://portal.azure.com';
        let storageEndpointSuffix;
        let azureVMFQDNSuffix;
        let activeDirectoryEndpointUrl;
        return this._client.resources.list({ filter: 'resourceType eq \'Microsoft.BingMaps/mapApis\'' })
            .then((resources) => {
            if (this._solutionType === 'remotemonitoring') {
                const armTemplatePath = __dirname + path.sep + 'solutions' + path.sep + this._solutionType + path.sep + 'armtemplates' + path.sep;
                this._parameters = require(armTemplatePath + this._sku + '-parameters.json');
                // using static map for China environment by default since Bing Map resource is not available.
                if (this._options.environment && this._options.environment.name === ms_rest_azure_1.AzureEnvironment.AzureChina.name) {
                    this._sku += '-static-map';
                }
                else if (answers.deploymentSku !== 'local') {
                    resources.forEach((resource) => {
                        if (resource.plan && resource.plan.name && resource.plan.name.toLowerCase() === 'internal1') {
                            freeBingMapResourceCount++;
                        }
                    });
                    if (freeBingMapResourceCount >= MAX_BING_MAP_APIS_FOR_INTERNAL1_PLAN) {
                        this._sku += '-static-map';
                    }
                }
                this._template = require(armTemplatePath + this._sku + '.json');
            }
            else {
                const armTemplatePath = __dirname + path.sep + 'solutions' + path.sep + this._solutionType + path.sep + 'armtemplate' + path.sep;
                this._template = require(armTemplatePath + 'template.json');
                this._parameters = require(armTemplatePath + 'parameters.json');
            }
            try {
                // Change the default suffix for basic sku based on current environment
                if (environment) {
                    switch (environment.name) {
                        case ms_rest_azure_1.AzureEnvironment.AzureChina.name:
                            azureVMFQDNSuffix = 'cloudapp.chinacloudapi.cn';
                            break;
                        case ms_rest_azure_1.AzureEnvironment.AzureGermanCloud.name:
                            azureVMFQDNSuffix = 'cloudapp.azure.de';
                            break;
                        case ms_rest_azure_1.AzureEnvironment.AzureUSGovernment.name:
                            azureVMFQDNSuffix = 'cloudapp.azure.us';
                            break;
                        default:
                            // use default parameter values of global azure environment
                            azureVMFQDNSuffix = 'cloudapp.azure.com';
                    }
                    storageEndpointSuffix = environment.storageEndpointSuffix;
                    activeDirectoryEndpointUrl = environment.activeDirectoryEndpointUrl;
                    if (storageEndpointSuffix.startsWith('.')) {
                        storageEndpointSuffix = storageEndpointSuffix.substring(1);
                    }
                    if (answers.deploymentSku === 'basic') {
                        this._parameters.storageEndpointSuffix = { value: storageEndpointSuffix };
                        this._parameters.vmFQDNSuffix = { value: azureVMFQDNSuffix };
                        this._parameters.aadInstance = { value: activeDirectoryEndpointUrl };
                    }
                }
                this.setupParameters(answers);
            }
            catch (ex) {
                throw new Error('Could not find template or parameters file, Exception:');
            }
            deployment.properties.parameters = this._parameters;
            deployment.properties.template = this._template;
            return deployment;
        })
            .then((properties) => {
            deployUI.start('Creating resource group');
            return this._client.resourceGroups.createOrUpdate(answers.solutionName, resourceGroup);
        })
            .then((result) => {
            resourceGroup = result;
            if (environment && environment.portalUrl) {
                portalUrl = environment.portalUrl;
            }
            resourceGroupUrl = `${portalUrl}/${answers.domainName}#resource${resourceGroup.id}`;
            console.log('Resources are being deployed at ' + resourceGroupUrl);
            deployUI.stop({ message: `Created resource group: ${chalk.cyan(resourceGroupUrl)}` });
            deployUI.start('Running validation before deploying resources');
            return this._client.deployments.validate(answers.solutionName, deploymentName, deployment);
        })
            .then((validationResult) => {
            if (validationResult.error) {
                const status = {
                    err: 'Deployment validation failed:\n' + JSON.stringify(validationResult.error, null, 2)
                };
                deployUI.stop(status);
                throw new Error(JSON.stringify(validationResult.error));
            }
            const options = {
                client: this._client,
                deploymentName,
                resourceGroupName: answers.solutionName,
                totalResources: deployment.properties.template.resources.length
            };
            deployUI.start('', options);
            return this._client.deployments.createOrUpdate(answers.solutionName, deploymentName, deployment);
        })
            .then((res) => {
            deployUI.stop();
            deploymentProperties = res.properties;
            if (answers.deploymentSku === 'standard') {
                deployUI.start(`Downloading credentials to setup Kubernetes from: ${chalk.cyan(deploymentProperties.outputs.masterFQDN.value)}`);
                return this.downloadKubeConfig(deploymentProperties.outputs, answers.sshFilePath);
            }
            if (answers.deploymentSku === 'local') {
                this.printEnvironmentVariables(deploymentProperties.outputs, storageEndpointSuffix);
            }
            return Promise.resolve('');
        })
            .then((kubeConfigPath) => {
            if (answers.deploymentSku === 'standard') {
                deployUI.stop({ message: `Credentials downloaded to config: ${chalk.cyan(kubeConfigPath)}` });
                const outputs = deploymentProperties.outputs;
                const config = new config_1.Config();
                config.AADTenantId = answers.aadTenantId;
                config.AADLoginURL = activeDirectoryEndpointUrl;
                config.ApplicationId = answers.appId;
                config.AzureStorageAccountKey = outputs.storageAccountKey.value;
                config.AzureStorageAccountName = outputs.storageAccountName.value;
                config.AzureStorageEndpointSuffix = storageEndpointSuffix;
                // If we are under the plan limi then we should have received a query key
                if (freeBingMapResourceCount < MAX_BING_MAP_APIS_FOR_INTERNAL1_PLAN) {
                    config.BingMapApiQueryKey = outputs.mapApiQueryKey.value;
                }
                config.DNS = outputs.agentFQDN.value;
                config.DocumentDBConnectionString = outputs.documentDBConnectionString.value;
                config.EventHubEndpoint = outputs.eventHubEndpoint.value;
                config.EventHubName = outputs.eventHubName.value;
                config.EventHubPartitions = outputs.eventHubPartitions.value.toString();
                config.IoTHubConnectionString = outputs.iotHubConnectionString.value;
                config.LoadBalancerIP = outputs.loadBalancerIp.value;
                config.Runtime = answers.runtime;
                config.TLS = answers.certData;
                const k8sMananger = new k8smanager_1.K8sManager('default', kubeConfigPath, config);
                deployUI.start('Setting up Kubernetes');
                return k8sMananger.setupAll();
            }
            return Promise.resolve();
        })
            .then(() => {
            if (answers.deploymentSku !== 'local') {
                const webUrl = deploymentProperties.outputs.azureWebsite.value;
                deployUI.start(`Waiting for ${chalk.cyan(webUrl)} to be ready, this could take up to 5 minutes`);
                return this.waitForWebsiteToBeReady(webUrl);
            }
            return Promise.resolve(true);
        })
            .then((done) => {
            const directoryPath = process.cwd() + path.sep + 'deployments';
            if (!fs.existsSync(directoryPath)) {
                fs.mkdirSync(directoryPath);
            }
            const fileName = directoryPath + path.sep + deploymentName + '-output.json';
            const troubleshootingGuide = 'https://aka.ms/iot-rm-tsg';
            if (answers.deploymentSku === 'local') {
                return Promise.resolve();
            }
            else if (deploymentProperties.outputs.azureWebsite) {
                const webUrl = deploymentProperties.outputs.azureWebsite.value;
                const status = {
                    message: `Solution: ${chalk.cyan(answers.solutionName)} is deployed at ${chalk.cyan(webUrl)}`
                };
                if (!done) {
                    status.message += `\n${chalk.yellow('Website not yet available, please refer to troubleshooting guide here:')}\n` +
                        `${chalk.cyan(troubleshootingGuide)}`;
                }
                deployUI.stop(status);
                const output = {
                    aadAppUrl: answers.aadAppUrl,
                    resourceGroupUrl,
                    troubleshootingGuide,
                    website: deploymentProperties.outputs.azureWebsite.value,
                };
                fs.writeFileSync(fileName, JSON.stringify(output, null, 2));
                console.log('Output saved to file: %s', `${chalk.cyan(fileName)}`);
                return Promise.resolve();
            }
            else {
                return Promise.reject('Azure website url not found in deployment output');
            }
        })
            .catch((error) => {
            let err = error.toString();
            console.log(err);
            if (err.includes('Entry not found in cache.')) {
                err = 'Session expired, Please run pcs login again.';
            }
            deployUI.stop({ err });
        });
    }
    downloadKubeConfig(outputs, sshFilePath) {
        if (!fs.existsSync(KUBEDIR)) {
            fs.mkdirSync(KUBEDIR);
        }
        const localKubeConfigPath = KUBEDIR + path.sep + 'config' + '-' + outputs.containerServiceName.value;
        const remoteKubeConfig = '.kube/config';
        const sshDir = sshFilePath.substring(0, sshFilePath.lastIndexOf(path.sep));
        const sshPrivateKeyPath = sshDir + path.sep + 'id_rsa';
        const pk = fs.readFileSync(sshPrivateKeyPath, 'UTF-8');
        const sshClient = new ssh2_1.Client();
        const config = {
            host: outputs.masterFQDN.value,
            port: 22,
            privateKey: pk,
            username: outputs.adminUsername.value
        };
        return new Promise((resolve, reject) => {
            let retryCount = 0;
            const timer = setInterval(() => {
                // First remove all listeteners so that we don't have duplicates
                sshClient.removeAllListeners();
                sshClient
                    .on('ready', (message) => {
                    sshClient.sftp((error, sftp) => {
                        if (error) {
                            sshClient.end();
                            reject(error);
                            clearInterval(timer);
                            return;
                        }
                        sftp.fastGet(remoteKubeConfig, localKubeConfigPath, (err) => {
                            sshClient.end();
                            clearInterval(timer);
                            if (err) {
                                reject(err);
                                return;
                            }
                            resolve(localKubeConfigPath);
                        });
                    });
                })
                    .on('error', (err) => {
                    if (retryCount++ > MAX_RETRY) {
                        clearInterval(timer);
                        reject(err);
                    }
                })
                    .on('timeout', () => {
                    if (retryCount++ > MAX_RETRY) {
                        clearInterval(timer);
                        reject(new Error('Failed after maximum number of tries'));
                    }
                })
                    .connect(config);
            }, 5000);
        });
    }
    setupParameters(answers) {
        this._parameters.solutionName.value = answers.solutionName;
        // Temporary check, in future both types of deployment will always have username and passord
        // If the parameters file has adminUsername section then add the value that was passed in by user
        if (this._parameters.adminUsername) {
            this._parameters.adminUsername.value = answers.adminUsername;
        }
        // If the parameters file has adminPassword section then add the value that was passed in by user
        if (this._parameters.adminPassword) {
            this._parameters.adminPassword.value = answers.adminPassword;
        }
        if (this._parameters.servicePrincipalSecret) {
            this._parameters.servicePrincipalSecret.value = answers.servicePrincipalSecret;
        }
        if (this._parameters.servicePrincipalClientId) {
            this._parameters.servicePrincipalClientId.value = answers.servicePrincipalId;
        }
        if (this._parameters.sshRSAPublicKey) {
            this._parameters.sshRSAPublicKey.value = fs.readFileSync(answers.sshFilePath, 'UTF-8');
        }
        if (this._parameters.azureWebsiteName) {
            this._parameters.azureWebsiteName.value = answers.azureWebsiteName;
        }
        if (this._parameters.remoteEndpointSSLThumbprint) {
            this._parameters.remoteEndpointSSLThumbprint.value = answers.certData.fingerPrint;
        }
        if (this._parameters.remoteEndpointCertificate) {
            this._parameters.remoteEndpointCertificate.value = answers.certData.cert;
        }
        if (this._parameters.remoteEndpointCertificateKey) {
            this._parameters.remoteEndpointCertificateKey.value = answers.certData.key;
        }
        if (this._parameters.aadTenantId) {
            this._parameters.aadTenantId.value = answers.aadTenantId;
        }
        if (this._parameters.aadClientId) {
            this._parameters.aadClientId.value = answers.appId;
        }
        if (this._parameters.microServiceRuntime) {
            this._parameters.microServiceRuntime.value = answers.runtime;
        }
    }
    waitForWebsiteToBeReady(url) {
        const status = url + '/ssl-proxy-status';
        const req = new fetch.Request(status, { method: 'GET' });
        let retryCount = 0;
        return new Promise((resolve, reject) => {
            const timer = setInterval(() => {
                fetch.default(req)
                    .then((value) => {
                    return value.json();
                })
                    .then((body) => {
                    if (body.Status.includes('Alive') || retryCount > MAX_RETRY) {
                        clearInterval(timer);
                        if (retryCount > MAX_RETRY) {
                            resolve(false);
                        }
                        else {
                            resolve(true);
                        }
                    }
                })
                    .catch((error) => {
                    // Continue
                    if (retryCount > MAX_RETRY) {
                        clearInterval(timer);
                        resolve(false);
                    }
                });
                retryCount++;
            }, 10000);
        });
    }
    printEnvironmentVariables(outputs, storageEndpointSuffix) {
        const data = [];
        data.push('PCS_IOTHUBREACT_ACCESS_CONNSTRING=' + outputs.iotHubConnectionString.value);
        data.push('PCS_IOTHUB_CONNSTRING=' + outputs.iotHubConnectionString.value);
        data.push('PCS_STORAGEADAPTER_DOCUMENTDB_CONNSTRING=' + outputs.documentDBConnectionString.value);
        data.push('PCS_TELEMETRY_DOCUMENTDB_CONNSTRING=' + outputs.documentDBConnectionString.value);
        data.push('PCS_TELEMETRYAGENT_DOCUMENTDB_CONNSTRING=' + outputs.documentDBConnectionString.value);
        data.push('PCS_IOTHUBREACT_HUB_ENDPOINT=Endpoint=' + outputs.eventHubEndpoint.value);
        data.push('PCS_IOTHUBREACT_HUB_PARTITIONS=' + outputs.eventHubPartitions.value);
        data.push('PCS_IOTHUBREACT_HUB_NAME=' + outputs.eventHubName.value);
        data.push('PCS_IOTHUBREACT_AZUREBLOB_ACCOUNT=' + outputs.storageAccountName.value);
        data.push('PCS_IOTHUBREACT_AZUREBLOB_KEY=' + outputs.storageAccountKey.value);
        data.push('PCS_IOTHUBREACT_AZUREBLOB_ENDPOINT_SUFFIX=' + storageEndpointSuffix);
        data.push('PCS_AUTH_REQUIRED=false');
        data.push('PCS_BINGMAP_KEY=static');
        console.log('Copy the following environment variables to /scripts/local/.env file: \n\ %s', `${chalk.cyan(data.join('\n'))}`);
    }
}
exports.DeploymentManager = DeploymentManager;
exports.default = DeploymentManager;
//# sourceMappingURL=deploymentmanager.js.map
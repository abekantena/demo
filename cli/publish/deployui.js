"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const chalk = require("chalk");
const inquirer = require("inquirer");
class DeployUI {
    constructor() {
        this.deploying = 'Deploying...';
        this.deployedResources = 'Deployed resources ';
        this.deployed = 'Deployed successfully';
        this.loader = [
            '/ ',
            '| ',
            '\\ ',
            '- ',
        ];
        this.i = 4;
        this.totalResourceCount = 0;
        this.completedResourceCount = 0;
        this.checkMark = `${chalk.green('\u2713 ')}`;
        this.crossMark = `${chalk.red('\u2715 ')}`;
        if (DeployUI._instance) {
            throw new Error('Error - use DeployUI.instance');
        }
        this.resourcesStatusAvailable = 0;
        DeployUI._instance = this;
    }
    static get instance() {
        if (!this._instance) {
            return new DeployUI();
        }
        return this._instance;
    }
    start(message, options) {
        this.clear();
        this.ui = new inquirer.ui.BottomBar();
        this.startTime = Date.now();
        this.timer = setInterval(() => {
            if (options) {
                options.client.deploymentOperations.list(options.resourceGroupName, options.deploymentName)
                    .then((value) => {
                    this.operationSet = new Set();
                    this.errorMessages = new Map();
                    const loader = this.loader[this.i++ % 4];
                    let operationsStatus = this.operationsStatusFormatter(value, loader);
                    if (operationsStatus) {
                        if (this.totalResourceCount > options.totalResources) {
                            options.totalResources = this.totalResourceCount;
                        }
                        const elapsedTime = new Date(Date.now() - this.startTime);
                        operationsStatus += loader + this.deployedResources +
                            `${chalk.cyan(this.completedResourceCount.toString(), 'of', options.totalResources.toString())}` + '\t(Elapsed Time: ' +
                            `${chalk.cyan(elapsedTime.getMinutes().toString(), 'minutes &', elapsedTime.getSeconds().toString(), 'seconds')})`;
                        this.ui.updateBottomBar(operationsStatus);
                    }
                    else {
                        this.ui.updateBottomBar(loader + this.deploying);
                    }
                })
                    .catch((err) => {
                    this.ui.updateBottomBar(this.loader[this.i++ % 4] + this.deploying);
                });
            }
            else {
                this.ui.updateBottomBar(this.loader[this.i++ % 4] + message);
            }
        }, 200);
    }
    stop(status) {
        clearInterval(this.timer);
        let message = '';
        if (this.errorMessages && this.errorMessages.size > 0) {
            message = this.crossMark + `${chalk.red('Deployment failed \n')}`;
            this.errorMessages.forEach((value) => {
                message += `${chalk.red(value)}` + '\n';
            });
        }
        else if (status) {
            if (status.err) {
                message = this.crossMark + `${chalk.red(status.err)}` + '\n';
            }
            else if (status.message) {
                message += this.checkMark + status.message + '\n';
            }
        }
        else {
            const totalTime = new Date(Date.now() - this.startTime);
            message += this.combinedStatus +
                this.checkMark + this.deployed + ', time taken: ' +
                `${chalk.cyan(totalTime.getMinutes().toString(), 'minutes &', totalTime.getSeconds().toString(), 'seconds')}` +
                '\n';
        }
        this.ui.updateBottomBar(message);
        this.close();
    }
    clear() {
        if (this.ui) {
            clearInterval(this.timer);
            this.ui.updateBottomBar('');
        }
    }
    close() {
        if (this.ui) {
            this.ui.close();
        }
    }
    operationsStatusFormatter(operations, loader) {
        const operationsStatus = [];
        this.combinedStatus = '';
        this.totalResourceCount = 0;
        this.completedResourceCount = 0;
        operations.forEach((operation) => {
            const props = operation.properties;
            const targetResource = props.targetResource;
            if (targetResource && targetResource.resourceType && targetResource.resourceName && !targetResource.actionName) {
                const key = targetResource.id;
                if (!this.operationSet.has(key)) {
                    this.totalResourceCount++;
                    this.operationSet.add(key);
                    let iconState = loader;
                    if (props.provisioningState === 'Succeeded') {
                        iconState = this.checkMark;
                        this.completedResourceCount++;
                    }
                    else if (props.provisioningState === 'Failed') {
                        iconState = this.crossMark;
                        const message = JSON.stringify(props.statusMessage, null, 2);
                        if (!this.errorMessages.has(key)) {
                            // Add the error messages to the map so that we can show it at the end
                            // of deployment, we don't want to cancel it because you can run it again
                            // to do incremental deployment that will save time.
                            this.errorMessages.set(key, message);
                        }
                    }
                    operationsStatus.push(iconState + 'Provisioning State: ' + props.provisioningState +
                        '\tResource Type: ' + targetResource.resourceType);
                }
            }
        });
        if (operationsStatus && operationsStatus.length) {
            // Sort so that we show the running state last
            operationsStatus.sort((first, second) => {
                const f = first.search('Succeeded');
                const s = second.search('Succeeded');
                if (f > s) {
                    return -1;
                }
                else if (s > f) {
                    return 1;
                }
                return 0;
            });
            operationsStatus.forEach((status) => {
                this.combinedStatus += status + '\n';
            });
        }
        return this.combinedStatus;
    }
}
exports.default = DeployUI;
//# sourceMappingURL=deployui.js.map
# OpenShift Pipelines Demo
This repository has all the files needed to set up a self-contained OpenShift Pipelines demonstration in a single OpenShift cluster.
# OpenShift Pipelines Installation
Install `Red Hat OpenShift Pipelines` via the web interface at Administrator->Operators->Operators Hub->OperatorHub. Use the default configuration values.

# Install gitea
`gitea` is used as the `git` repository for this demo. Install it by running the following commands in a `bash` shell:
Create a project/namespace for `gitea`:
```bash
oc new-project gitea
```
Add privileges for `gitea` (not recommended for production systems):
```bash
oc adm policy add-scc-to-user privileged -z default
oc adm policy add-scc-to-user nonroot -z gitea-memcached
```
Use `helm` to install `gitea`:
```bash
helm install gitea gitea-charts/gitea --set ingress.hosts[0].host=gitea-http-gitea$(oc whoami --show-console | sed "s/.*console-openshift-console//") --set gitea.config.webhook.ALLOWED_HOST_LIST='*' --set gitea.config.webhook.SKIP_TLS_VERIFY=true --set image.pullPolicy=IfNotPresent
```
Create a `route` for `gitea`:
```bash
oc expose service gitea-http
oc get route gitea-http -o jsonpath='{"http://"}{.status.ingress[0].host}{"\n"}'
```
When all pods are in the `Running` status, browse the the URL displayed above and sign into `gitea` using the following credentials:
* user: gitea_admin
* password: r8sA8CPHD9!bt6d

In `gitea` press the pull-down icon at the top right and select `Site Administration`. In the `User Accounts` tab press `Create User Account` and create a user named `demo` with password `demodemo`. Deselect `Require user to change password`.

# Set up Git repository
* `Sign Out` of `gitea` and then log in using the `demo` account created above. Create a repository named `httpserver`.
* Copy the `httpserver` directory from this repository to a new location.
* In the copied directory run the instructions provided by `gitea` for creating a new repository with the following changes.
  * Instead of `git add README.md` use `git add .`
  * At this time, the `gitea` instructions state `git push -u origin master`. Replace this with `git push -u origin main`.
* During the `git push` step you will be prompted for the user and password (demo/demodemo).

Repeat the above steps for a repository named `httpserver-cd`.


# Create a project/namespace for OpenShift Pipelines

Create a project for OpenShift Pipelines by running:
```bash
oc new-project cicd
```
Create the OpenShift Pipeline for this demo by running:
```bash
oc create -n cicd -f tekton/linter-task.yaml
oc create -n cicd -f tekton/pipeline.yaml
```

# Create a Secret for Gitea
Create a secret for Gitea by running the following:
```bash
oc create -n cicd -f - <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: gitea-credentials
  annotations:
    tekton.dev/git-0: http://gitea-http-gitea$(oc whoami --show-console | sed "s/.*console-openshift-console//")
type: kubernetes.io/basic-auth
stringData:
  username: demo
  password: demodemo
EOF
```

# Create a ServiceAccount
Create a ServiceAccount for the OpenShift Pipelines builder as follows:
```bash
oc create -n cicd -f - <<EOF
apiVersion: v1
kind: ServiceAccount
metadata:
  name: build-bot
secrets:
  - name: gitea-credentials
EOF
```
Add a CluterRole binding for the builder ServiceAccount (not recommended for production clusters):
```bash
oc adm policy add-cluster-role-to-user tekton-triggers-eventlistener-clusterroles system:serviceaccount:cicd:build-bot
oc adm policy add-role-to-user tekton-triggers-eventlistener-roles system:serviceaccount:cicd:build-bot
```
Temporarily apply this hack (not on production systems). This will be resolved in the future:
```
oc adm policy add-cluster-role-to-user cluster-admin -z build-bot
```

# Create an EventListener
Create an OpenShift Pipelines EventListener for this demo by running:
```bash
oc create -n cicd -f tekton/gitea/gitea-binding.yaml
oc create -n cicd -f tekton/gitea/gitea-template.yaml
oc create -n cicd -f tekton/gitea/gitea-eventlistener.yaml
```
# Create a Gitea Webhook
Browse to the `gitea` `httpserver` repository, select the `Settings` tab and in `Webhooks` press `Add Webhook`. Create a `Gitea` webhook with target URL:
```
http://el-gitea-eventlistener.cicd.svc.cluster.local:8080
```
(Note that this URL works for this demo as `gitea` is running in the same cluster as the OpenShift Pipelines event listener.)

# Test it Out
In the `httpserver` repository, update a file, commit and push the changes.
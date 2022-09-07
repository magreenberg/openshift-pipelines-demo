# OpenShift Pipelines Demo
This repository has all the files needed to set up a self-contained OpenShift Pipelines demonstration in a single OpenShift cluster.
# OpenShift Pipelines Installation
Install OpenShift Pipelines via Operators Hub.

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
helm install gitea gitea-charts/gitea --set ingress.hosts[0].host=gitea-http-gitea$(oc whoami --show-console | sed "s/.*console-openshift-console//") --set gitea.config.webhook.ALLOWED_HOST_LIST='*' --set gitea.config.webhook.SKIP_TLS_VERIFY=true
```
Create a `route` for `gitea`:
```bash
oc expose service gitea-http
oc get route gitea-http -o jsonpath='{"http://"}{.status.ingress[0].host}{"\n"}'
```
Browse the the URL displayed above and sign into `gitea` using the following credentials:
* user: gitea_admin
* password: r8sA8CPHD9!bt6d

In `gitea` create a user named `demo` with password `demodemo`.

# Set up Git repository
Sign into `gitea` using the `demo` account and create two repositories:
* httpserver
* httpserver-cd

Follow the `gitea` instructions to add the code from the `httpserver` and `httpserver-cd` directories to the `gitea` repositories.

# Create a project/namespace for OpenShift Pipelines

Create a project for OpenShift Pipelines by running:
```bash
oc new-project cicd
```
Create the OpenShift Pipeline for this demo by running:
```bash
oc create -f tekton/linter-task.yaml
oc create -f tekton/pipeline.yaml
```

# Create a Secret for Gitea
Create a secret for Gitea by running the following:
```bash
oc create -f - <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: gitea-credentials
  annotations:
    tekton.dev/git-0: gitea-http-gitea$(oc whoami --show-console | sed "s/.*console-openshift-console//")
type: kubernetes.io/basic-auth
stringData:
  username: demo
  password: demodemo
EOF
```

# Create a ServiceAccount
Create a ServiceAccount for the OpenShift Pipelines builder as follows:
```bash
oc create -f - <<EOF
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
oc adm policy add-cluster-role-to-user cluster-admin -z build-bot
```

# Create an EventListener
Create an OpenShift Pipelines EventListener for this demo by running:
```bash
oc create -f tekton/gitea-binding.yaml
oc create -f tekton/gitea-template.yaml
oc create -f tekton/gitea-eventlistener.yaml
```
# Create a Gitea Webhook
In `gitea` select the `Settings` tab and in `Webhooks` create a webhook with target URL:
```
http://el-gitea-eventlistener.cicd.svc.cluster.local:8080
```
(Note that this URL works for this demo as `gitea` is running in the same cluster as the OpenShift Pipelines event listener.)

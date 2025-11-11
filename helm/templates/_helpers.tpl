{{/*
Expand the name of the chart.
*/}}
{{- define "idxworker.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "idxworker.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "idxworker.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "idxworker.labels" -}}
helm.sh/chart: {{ include "idxworker.chart" . }}
{{ include "idxworker.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "idxworker.selectorLabels" -}}
app.kubernetes.io/name: {{ include "idxworker.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "idxworker.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "idxworker.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Check to see if '.Values.global "dataone-indexer.enabled' is set to true in the top-level metacat
chart. If so, we know we're running as a subchart, and can infer the 'metacat.fullname'.
If we're not running as a sub-chart, then we need to read the user-provided value from
.Values.idxworker.metacatK8sFullName
*/}}
{{- define "get.metacat.fullname" -}}
{{- if (index .Values.global "dataone-indexer.enabled") }}
{{- if contains "metacat" (lower .Release.Name) }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-metacat" .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- else }}
{{- default "MISSING-idxworker.metacatK8sFullName-MISSING" .Values.idxworker.metacatK8sFullName }}
{{- end }}
{{- end }}

{{/*
set MN url
If we're running as a subchart, can use direct access without needing to go through ingress/https;
  e.g. http://metacatbrooke-hl:8080/metacat/d1/mn
If connecting to an instance outside the cluster, should use https;
  e.g. https://metacat-dev.test.dataone.org/metacat/d1/mn
*/}}
{{- define "idxworker.mn.url" -}}
{{- $mn_url := .Values.idxworker.mn_url }}
{{- if not $mn_url }}
{{- $fullname := (include "get.metacat.fullname" .) }}
{{- $mn_url = printf "http://%s-hl:8080/%s/d1/mn" $fullname .Values.global.metacatAppContext }}
{{- end }}
{{- $mn_url }}
{{- end }}

{{/*
set Claim Name of existing PVC to use (typically the volume that is shared with metacat)
Either use the value set in .Values.persistence.claimName, or if blank, autopopulate with
  {podname}-metacat-{fullName}-0 (e.g. metacatbrooke-metacat-metacatbrooke-metacat-0)
  (see 'idxworker.fullname' for logic behind how a 'fullname' is defined)
*/}}
{{- define "idxworker.shared.claimName" -}}
{{- $claimName := .Values.persistence.claimName }}
{{- if not $claimName }}
{{- $fullname := (include "get.metacat.fullname" .) }}
{{- $claimName = printf "%s-metacat-%s-0" .Release.Name $fullname }}
{{- end }}
{{- $claimName }}
{{- end }}

{{/*
Create a default fully qualified app name for the embedded RabbitMQ Cluster Operator Deployment.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "idxworker.rmq.fullname" -}}
{{- $name := default "rmq" .Values.rabbitmq.nameOverride }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
If RabbitMQ Secret Name not defined, infer from bundled RMQ Cluster Operator, or error out.
*/}}
{{- define "idxworker.rabbitmq.secret.name" }}
{{- $rmqSecret := .Values.idxworker.rabbitmqSecret }}
{{- if and ((.Values.rabbitmq).enabled) (not $rmqSecret) }}
{{- $rmqSecret = printf "%s-default-user" (include "idxworker.rmq.fullname" .) }}
{{- end }}
{{- required "idxworker.rabbitmqSecret REQUIRED if not using bundled RMQ Operator" $rmqSecret }}
{{- end }}

{{/*
If RabbitMQ username not defined, infer from bundled RMQ Cluster Operator secret, or error out.
*/}}
{{- define "idxworker.rabbitmq.user" }}
{{- $rmqUser := .Values.idxworker.rabbitmqUsername }}
{{- if and ((.Values.rabbitmq).enabled) (not $rmqUser) }}
{{- $key := .Values.idxworker.rabbitmqUserKey | default "username" }}
{{- $secrets := (include "idxworker.rabbitmq.secret.name" .) }}
{{- $secretData := (lookup "v1" "Secret" .Release.Namespace $secrets).data | default dict -}}
{{- $rmqUser = ((get $secretData $key) | b64dec) }}
{{- end }}
{{- required "idxworker.rabbitmqUsername REQUIRED if not using bundled RMQ Operator" $rmqUser }}
{{- end }}

{{/*
set RabbitMQ HostName
*/}}
{{- define "idxworker.rabbitmq.hostname" }}
{{- $rmqHost := .Values.idxworker.rabbitmqHostname }}
{{- if and ((.Values.rabbitmq).enabled) (not $rmqHost) }}
{{- $rmqHost = (include "idxworker.rmq.fullname" .) }}
{{- end }}
{{- required "idxworker.rabbitmqHostname REQUIRED if not using bundled RMQ Operator" $rmqHost }}
{{- end }}

{{/*
set RabbitMQ HostPort
*/}}
{{- define "idxworker.rabbitmq.hostport" }}
{{- $rmqPort := .Values.idxworker.rabbitmqHostPort }}
{{- if and ((.Values.rabbitmq).enabled) (not $rmqPort) -}}
{{ $rmqPort = .Values.rabbitmq.service.ports.amqp }}
{{- end }}
{{- $rmqPort }}
{{- end }}

{{/*
set Solr HostName
*/}}
{{- define "idxworker.solr.hostname" -}}
{{- $solrHost := .Values.idxworker.solrHostname }}
{{- if and (or (((.Values.global).solr).enabled) ((.Values.solr).enabled)) (not $solrHost) -}}
    {{- $solrHost = printf "%s-solr-headless" .Release.Name -}}
{{- end }}
{{- $solrHost }}
{{- end }}

{{/*
Renders a value that contains template.
Usage:
{{ include "idxhelpers.tplvalues.render" ( dict "value" .Values.path.to.the.Value "context" $) }}
*/}}
{{- define "idxhelpers.tplvalues.render" -}}
    {{- if typeIs "string" .value }}
        {{- tpl .value .context }}
    {{- else }}
        {{- tpl (.value | toYaml) .context }}
    {{- end }}
{{- end -}}

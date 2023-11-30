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
set MN url
If we're running as a subchart, can use direct access without needing to go through ingress/https;
  e.g. http://metacatbrooke-hl:8080/metacat/d1/mn
If connecting to an instance outside the cluster, should use https;
  e.g. https://metacat-dev.test.dataone.org/metacat/d1/mn
*/}}
{{- define "idxworker.mn.url" -}}
{{- $mn_url := .Values.idxworker.mn_url }}
{{- if not $mn_url }}
{{- $mn_url = printf "http://%s-hl:8080/%s/d1/mn" .Release.Name .Values.global.metacatAppContext }}
{{- end }}
{{- $mn_url }}
{{- end }}

{{/*
set Claim Name of existing PVC to use (typically the volume that is shared with metacat)
Either use the value set in .Values.persistence.claimName, or if blank, autopopulate with
  {podname}-metacat-{releaseName}-0 (e.g. metacatbrooke-metacat-metacatbrooke-0)
*/}}
{{- define "idxworker.shared.claimName" -}}
{{- $claimName := .Values.persistence.claimName }}
{{- if not $claimName }}
{{- $claimName = .Release.Name }}-metacat-{{- .Release.Name }}-0
{{- end }}
{{- $claimName }}
{{- end }}

{{/*
Check if RabbitMQ SubChart is enabled
*/}}
{{- define "rmq.enabled" -}}
{{ $rmqEnabled := (or (((.Values.global).rabbitmq).enabled) ((.Values.rabbitmq).enabled)) }}
{{ end }}

{{/*
set RabbitMQ HostName
*/}}
{{- define "idxworker.rabbitmq.hostname" -}}
{{- $rmqHost := .Values.idxworker.rabbitmqHostname }}
{{- if and (include "rmq.enabled" .) (not $rmqHost) -}}
{{- $rmqHost = printf "%s-rabbitmq-headless" .Release.Name -}}
{{- end }}
{{- $rmqHost }}
{{- end }}

{{/*
set RabbitMQ HostPort
*/}}
{{- define "idxworker.rabbitmq.hostport" }}
{{- $rmqPort := .Values.idxworker.rabbitmqHostPort }}
{{- if and (include "rmq.enabled" .) (not $rmqPort) -}}
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

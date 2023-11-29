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
e.g. https://metacat-dev.test.dataone.org/metacat/d1/mn
*/}}
{{- define "idxworker.mn.url" -}}
{{- if not .Values.idxworker.mn_url }}
{{- printf "https://%s-metacat-hl/%s/d1/mn" .Release.Name .Values.global.metacatAppContext }}
{{- else }}
{{- .Values.idxworker.mn_url }}
{{- end }}
{{- end }}

{{/*
set Claim Name of existing PVC to use (typically the volume that is shared with metacat)
Either use the value set in .Values.persistence.claimName, or if blank, autopopulate with
  {podname}-metacat-{releaseName}-0 (e.g. metacatbrooke-metacat-metacatbrooke-0)
*/}}
{{- define "idxworker.shared.claimName" -}}
{{- if not .Values.persistence.claimName }}
{{- .Release.Name }}-metacat-{{- .Release.Name }}-0
{{- else }}
{{- .Values.persistence.claimName }}
{{- end }}
{{- end }}

{{/*
set RabbitMQ HostName
*/}}
{{- define "idxworker.rabbitmq.hostname" -}}
{{- if not .Values.rabbitmq.hostname }}
{{- .Release.Name }}-rabbitmq-headless
{{- else }}
{{- .Values.rabbitmq.hostname }}
{{- end }}
{{- end }}

{{/*
set Solr HostName
*/}}
{{- define "idxworker.solr.hostname" -}}
{{- if not .Values.solr.hostname }}
{{- .Release.Name }}-solr-headless
{{- else }}
{{- .Values.solr.hostname }}
{{- end }}
{{- end }}

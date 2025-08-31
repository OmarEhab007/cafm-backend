{{/*
Expand the name of the chart.
*/}}
{{- define "cafm-backend.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "cafm-backend.fullname" -}}
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
{{- define "cafm-backend.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "cafm-backend.labels" -}}
helm.sh/chart: {{ include "cafm-backend.chart" . }}
{{ include "cafm-backend.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: backend
app.kubernetes.io/part-of: cafm-system
{{- end }}

{{/*
Selector labels
*/}}
{{- define "cafm-backend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "cafm-backend.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "cafm-backend.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "cafm-backend.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create database URL
*/}}
{{- define "cafm-backend.databaseUrl" -}}
{{- if .Values.postgresql.enabled }}
{{- printf "jdbc:postgresql://%s-postgresql:5432/%s" .Release.Name .Values.postgresql.auth.database }}
{{- else if .Values.externalServices.postgresql.host }}
{{- printf "jdbc:postgresql://%s:%v/%s" .Values.externalServices.postgresql.host .Values.externalServices.postgresql.port .Values.externalServices.postgresql.database }}
{{- end }}
{{- end }}

{{/*
Create Redis URL
*/}}
{{- define "cafm-backend.redisUrl" -}}
{{- if .Values.redis.enabled }}
{{- printf "redis://%s-redis-master:6379" .Release.Name }}
{{- else if .Values.externalServices.redis.host }}
{{- printf "redis://%s:%v" .Values.externalServices.redis.host .Values.externalServices.redis.port }}
{{- end }}
{{- end }}
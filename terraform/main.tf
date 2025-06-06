variable "project_id" {
  description = "GCP project ID"
  type        = string
  default     = "actin-training"
}

variable "region" {
  description = "GCP region"
  type        = string
  default     = "europe-west4"
}

provider "google" {
  project = var.project_id
  region  = var.region
}

data "google_project" "project" {
  project_id = var.project_id
}

resource "google_project_iam_member" "reader_on_actin_build" {
  project = "actin-build"
  role    = "roles/artifactregistry.reader"
  member  = "serviceAccount:service-${data.google_project.project.number}@container-engine-robot.iam.gserviceaccount.com"
}

resource "google_container_cluster" "autopilot_cluster" {
  name = "actin-autopilot-cluster"
  location = var.region
  enable_autopilot = true
}
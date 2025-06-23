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

#####################
# pub/sub
resource "google_pubsub_topic" "gcp-training-denis" {
  name    = "gcp-training-denis"
  project = var.project_id
}

resource "google_pubsub_subscription" "gcp-training-denis-subscription" {
  name  = "gcp-training-denis-subscription"
  project = var.project_id
  topic = google_pubsub_topic.gcp-training-denis.id
  ack_deadline_seconds = 10  
  message_retention_duration = "86400s"
}

resource "google_service_account" "pubsub_sa" {
  account_id   = "pubsub-workload-sa"  
  project      = var.project_id
}

resource "google_pubsub_topic_iam_member" "pubsub_publisher" {
  project = var.project_id
  topic   = google_pubsub_topic.gcp-training-denis.name
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.pubsub_sa.email}"
}

resource "google_pubsub_subscription_iam_member" "pubsub_subscriber" {
  project      = var.project_id
  subscription = google_pubsub_subscription.gcp-training-denis-subscription.name
  role         = "roles/pubsub.subscriber"
  member       = "serviceAccount:${google_service_account.pubsub_sa.email}"
}

resource "google_service_account_iam_binding" "workload_identity_binding" {
  service_account_id = google_service_account.pubsub_sa.name
  role               = "roles/iam.workloadIdentityUser"
  members = [
    "serviceAccount:${var.project_id}.svc.id.goog[default/pubsub-workload-sa]"
  ]
}

#####################
#bucket
resource "google_storage_bucket" "gcp-training-denis" {
  name     = "gcp-training-denis"
  project  = var.project_id
  location = var.region
}

resource "google_storage_bucket_iam_member" "pubsub_sa_storage_writer" {
  bucket = google_storage_bucket.gcp-training-denis.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.pubsub_sa.email}"
}

# Kubernetes Hosting Marktrecherche fuer Travelmate (Stand 2026-03-26)

## Ziel

Diese Recherche vergleicht aktuelle Kubernetes-Hosting-Angebote fuer den produktiven Betrieb von Travelmate und leitet daraus eine pragmatische Empfehlung fuer Kosten, Betriebsaufwand und Migrationsrisiko ab.

## Ausgangslage im Repository

Der aktuelle lokale Stack basiert auf Docker Compose und umfasst:

- 4 PostgreSQL-Instanzen (`iam`, `trips`, `expense`, `keycloak`)
- Keycloak
- RabbitMQ
- Gateway
- IAM
- Trips
- Expense
- optional Ollama fuer LLM-gestuetzte Funktionen

Fuer die guenstigste Betriebsform ist relevant, dass Ollama in den Applikationen optional ist (`LLM_ENABLED=false`), waehrend Datenbanken, Keycloak und RabbitMQ betriebsnotwendig sind.

## Bewertungsrahmen

Verglichen wurden die Anbieter nach folgenden Kriterien:

- Einstiegskosten fuer einen kleinen produktiven Cluster
- Eignung fuer den kompletten Travelmate-Stack inklusive Stateful Workloads
- Aufwand fuer CI/CD und Deployment
- Europaeische Verfuegbarkeit und operative Einfachheit
- Eignung fuer Hobby-, Demo- und kleine Produktionsumgebungen

## Marktvergleich

| Option | Betriebsmodell | Aktuelle Marktindikation | Eignung fuer Travelmate | Bewertung |
|---|---|---|---|---|
| Oracle Cloud Always Free + k3s | self-managed | Always Free ARM Compute mit bis zu 4 OCPU, 24 GB RAM und 200 GB Block Volume | Technisch moeglich fuer kleinen Single-Node-Cluster | Billigste Option, aber mit Free-Tier-Risiko |
| Hetzner Cloud + k3s | self-managed | Guenstige Cloud-VMs; Preisanpassung zum 2026-04-01 angekuendigt | Sehr guter Fit fuer kleinen Produktivbetrieb | Beste Preis/Leistung |
| Civo Managed Kubernetes | managed | Einstieg ab 5.43 USD/Monat, Control Plane kostenlos | Solide Managed-Variante fuer kleine Cluster | Beste Managed-Kubernetes-Option im Low-Cost-Segment |
| OVHcloud Managed Kubernetes | managed | Control Plane kostenlos, Worker und Volumes werden separat berechnet | Gute EU-Option mit planbaren Kosten | Preislich attraktiv, aber komplexer als Hetzner+k3s |
| Linode Kubernetes Engine (LKE) | managed | Basis-Control-Plane kostenlos, Worker-Nodes auf regulaerer Compute-Basis | Solide, aber fuer Travelmate nicht der guenstigste Weg | Akzeptabel, aber nicht Top-Empfehlung |

## Detaillierte Bewertung

### 1. Oracle Cloud Always Free + k3s

Vorteile:

- nominell 0 USD/Monat
- genug Ressourcen fuer einen kleinen Single-Node-k3s-Cluster
- CI/CD kann mit GitHub Actions und Self-Hosted Runner auf derselben VM umgesetzt werden

Nachteile:

- ARM-only fuer die attraktive Free-Tier-Variante
- Free-Tier-Risiko bei Verfuegbarkeit und Langzeitzuverlaessigkeit
- hoeherer Eigenaufwand fuer Netzwerk, Storage, Backups und Upgrades

Einschaetzung:

Die Plattform ist fuer Demo-, Hobby- oder interne Testsysteme die billigste Kubernetes-Variante. Fuer verlaesslichen Produktivbetrieb ist sie nur bedingt empfehlenswert.

### 2. Hetzner Cloud + k3s

Vorteile:

- deutlich guenstiger als Managed-Kubernetes-Angebote
- sehr guter Fit fuer den existierenden Compose-basierten Travelmate-Stack
- einfacher Einstieg mit Single-Node-k3s moeglich
- spaeterer Ausbau auf getrennte Control-Plane- und Worker-Nodes moeglich

Nachteile:

- Kubernetes-Betrieb bleibt eigenes Thema
- Stateful Workloads im Cluster muessen bewusst abgesichert werden
- kein Managed Control Plane

Einschaetzung:

Das ist die guenstigste ernsthaft empfehlenswerte Kubernetes-Option fuer Travelmate. Fuer den Start ist ein kleiner k3s-Cluster auf Hetzner ausreichend. Bei steigenden Anforderungen kann schrittweise skaliert werden.

### 3. Civo Managed Kubernetes

Vorteile:

- kostenloses Control Plane
- sehr klarer Low-Cost-Einstieg
- Kubernetes-Fokus und einfache Bedienung

Nachteile:

- Worker, Persistent Volumes und Zusatzdienste kosten extra
- fuer den kompletten Travelmate-Stack immer noch deutlich teurer als eine einzelne Hetzner-VM

Einschaetzung:

Civo ist die attraktivste Managed-Kubernetes-Option, wenn ein selbstverwalteter Cluster vermieden werden soll. Fuer Travelmate ist es aber eher die guenstigste Managed- als die guenstigste Gesamtoption.

### 4. OVHcloud Managed Kubernetes

Vorteile:

- kostenloses Managed Control Plane
- europaeischer Anbieter
- solide Basis fuer produktive Kubernetes-Setups

Nachteile:

- Gesamtkosten entstehen ueber Worker, Storage und Netzwerktopologie
- fuer kleine Teams operativ meist schwergewichtiger als noetig

Einschaetzung:

OVH ist eine gute Alternative, wenn ein europaeischer Managed-Kubernetes-Anbieter bevorzugt wird. Im direkten Vergleich bleibt Hetzner+k3s fuer Travelmate wirtschaftlicher.

### 5. Linode Kubernetes Engine

Vorteile:

- kostenloses Basis-Control-Plane
- einfaches Managed-Angebot

Nachteile:

- fuer kleine europaeische Deployments meist kein Kosten- oder Einfachheitsvorteil gegenueber Civo oder Hetzner

Einschaetzung:

LKE ist technisch geeignet, aber kein besonders starker Preis-Leistungs-Sieger fuer den Travelmate-Fall.

## Empfehlung

### Empfehlung A: guenstigste Kubernetes-Loesung insgesamt

**Oracle Cloud Always Free + Single-Node k3s**

Geeignet fuer:

- Hobby
- Demo
- internes Staging
- kostengetriebenes Experimentieren mit Kubernetes

Nicht erste Wahl fuer:

- verlaessliche produktive Umgebungen
- SLA-nahe Erwartungen
- Teams mit wenig Plattformzeit

### Empfehlung B: guenstigste sinnvoll empfehlenswerte Kubernetes-Loesung

**Hetzner Cloud + k3s**

Empfohlene Startform:

- 1 kleiner bis mittlerer Node fuer ersten produktiven Betrieb
- Ingress Controller
- cert-manager
- externe Backups fuer PostgreSQL
- GitHub Actions fuer CI
- Self-Hosted Runner oder SSH-basierter Deploy fuer CD

Begruendung:

- niedrigste realistische Gesamtkosten
- gute Passung zum bestehenden Docker- und Compose-Modell
- keine hyperscaler-typische Preislogik
- saubere Migrationsbasis fuer spaetere Trennung von Stateful und Stateless Komponenten

### Empfehlung C: guenstigste Managed-Kubernetes-Option

**Civo Managed Kubernetes**

Geeignet, wenn:

- Kubernetes gewuenscht ist
- Self-Management minimiert werden soll
- etwas hoehere Kosten gegenueber Hetzner akzeptabel sind

## Travelmate-spezifische Empfehlung fuer die Migration

Fuer Travelmate sollte die Migration nicht als Big Bang erfolgen. Sinnvoll ist ein gestuftes Zielbild:

1. Zunaechst nur die stateless Services auf Kubernetes heben:
   - gateway
   - iam
   - trips
   - expense
2. RabbitMQ und Keycloak in Phase 1 entweder weiterhin dediziert betreiben oder sehr bewusst als Stateful Workloads modellieren.
3. PostgreSQL nicht als erste Migrationswelle in Kubernetes ziehen, sondern erst nach Backup-, Restore- und Storage-Konzept.
4. Ollama nicht Teil der ersten Kubernetes-Migration machen; fuer die Kostenoptimierung deaktiviert lassen.

Diese Reihenfolge reduziert Risiko, weil Travelmate mehrere zustandsbehaftete Kernkomponenten besitzt und Kubernetes den operativen Aufwand fuer Stateful Workloads deutlich erhoeht.

## CI/CD-Empfehlung fuer den Kubernetes-Zielbetrieb

Empfohlenes Minimum:

- GitHub Actions fuer Build und Test
- Container-Build pro SCS
- Container-Registry erst einfuehren, wenn der Kubernetes-Betrieb aktiv umgesetzt wird
- Deployment via Helm oder Kustomize
- getrennte Stages fuer `ci`, `staging`, `production`

Fuer kleine Teams ist `GitHub Actions + Kustomize` der einfachste Einstieg. Helm lohnt sich, wenn mehrere Umgebungen oder konfigurierbare Releases benoetigt werden.

## Entscheidung in Kurzform

| Ziel | Empfehlung |
|---|---|
| Billigste Kubernetes-Option | Oracle Cloud Always Free + k3s |
| Billigste sinnvoll empfehlenswerte Option | Hetzner Cloud + k3s |
| Billigste Managed-Kubernetes-Option | Civo Managed Kubernetes |

## Quellen

- Oracle Cloud Always Free Resources: <https://docs.oracle.com/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm>
- Hetzner Preisankuendigung ab 2026-04-01: <https://www.hetzner.com/pressroom/statement-price-adjustment/>
- Civo Kubernetes Pricing: <https://www.civo.com/public-cloud/kubernetes>
- OVHcloud Public Cloud Prices / Managed Kubernetes: <https://us.ovhcloud.com/public-cloud/prices/>
- Linode Kubernetes Engine Pricing: <https://techdocs.akamai.com/cloud-computing/docs/linode-kubernetes-engine>
- GitHub Actions Billing: <https://docs.github.com/en/billing/managing-billing-for-github-actions/about-billing-for-github-actions>
- GitHub Self-Hosted Runners: <https://docs.github.com/en/actions/concepts/runners/self-hosted-runners>

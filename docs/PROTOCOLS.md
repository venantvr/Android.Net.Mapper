# Documentation des Protocoles Réseau

> Guide technique des protocoles utilisés par NetMapper

---

## Table des matières

1. [Vue d'ensemble](#vue-densemble)
2. [Protocoles de découverte](#protocoles-de-découverte)
   - [ARP](#arp---address-resolution-protocol)
   - [ICMP](#icmp---internet-control-message-protocol)
   - [mDNS/Bonjour](#mdnsbonjour---multicast-dns)
   - [UPnP/SSDP](#upnpssdp---universal-plug-and-play)
   - [SNMP](#snmp---simple-network-management-protocol)
3. [Protocoles de scan](#protocoles-de-scan)
   - [TCP Connect Scan](#tcp-connect-scan)
   - [Banner Grabbing](#banner-grabbing)
4. [Protocoles applicatifs](#protocoles-applicatifs)
   - [Wake-on-LAN](#wake-on-lan)
   - [SSL/TLS](#ssltls)
5. [Ports standards](#ports-standards)
6. [Sécurité et bonnes pratiques](#sécurité-et-bonnes-pratiques)

---

## Vue d'ensemble

NetMapper utilise plusieurs protocoles réseau pour découvrir et identifier les appareils sur un réseau local. Chaque protocole a un rôle spécifique et fournit des informations complémentaires.

```
┌─────────────────────────────────────────────────────────────┐
│                    NetMapper - Protocoles                    │
├─────────────────────────────────────────────────────────────┤
│  Couche 2 (Liaison)     │  ARP                              │
│  Couche 3 (Réseau)      │  ICMP, IP                         │
│  Couche 4 (Transport)   │  TCP, UDP                         │
│  Couche 7 (Application) │  mDNS, UPnP, SNMP, HTTP, SSH...   │
└─────────────────────────────────────────────────────────────┘
```

---

## Protocoles de découverte

### ARP - Address Resolution Protocol

#### Description
ARP (Address Resolution Protocol) est un protocole de couche 2 qui permet de faire correspondre une adresse IP à une adresse MAC (Media Access Control) sur un réseau local.

#### Caractéristiques techniques
| Paramètre | Valeur |
|-----------|--------|
| Couche OSI | 2 (Liaison de données) |
| Type | Broadcast / Unicast |
| Fichier système | `/proc/net/arp` |

#### Fonctionnement dans NetMapper
NetMapper lit la table ARP du système Android pour enrichir les informations des appareils découverts :

```
IP address       HW type     Flags       HW address            Mask     Device
192.168.1.1      0x1         0x2         aa:bb:cc:dd:ee:ff     *        wlan0
```

#### Informations obtenues
- **Adresse MAC** : Identifiant unique de l'interface réseau
- **Fabricant** : Déduit des 3 premiers octets (OUI - Organizationally Unique Identifier)

#### Limitations
- Fonctionne uniquement sur le réseau local (même sous-réseau)
- La table ARP peut être incomplète si aucune communication récente n'a eu lieu
- Nécessite que l'appareil ait répondu à une requête réseau

---

### ICMP - Internet Control Message Protocol

#### Description
ICMP est un protocole de couche 3 utilisé pour le diagnostic et le signalement d'erreurs réseau. La commande `ping` utilise ICMP Echo Request/Reply.

#### Caractéristiques techniques
| Paramètre | Valeur |
|-----------|--------|
| Couche OSI | 3 (Réseau) |
| Protocole IP | 1 |
| Types utilisés | 8 (Echo Request), 0 (Echo Reply) |

#### Fonctionnement dans NetMapper
```
┌─────────┐    ICMP Echo Request (Type 8)    ┌─────────┐
│ Scanner │ ─────────────────────────────────▶│  Hôte   │
│         │◀───────────────────────────────── │         │
└─────────┘    ICMP Echo Reply (Type 0)      └─────────┘
```

#### Informations obtenues
- **Disponibilité** : L'hôte est-il joignable ?
- **Latence (RTT)** : Temps aller-retour en millisecondes
- **TTL** : Time To Live, indicateur potentiel du système d'exploitation

#### Limitations
- Certains pare-feu bloquent ICMP
- Android utilise `InetAddress.isReachable()` qui peut utiliser TCP si ICMP échoue
- Pas d'information sur les services disponibles

---

### mDNS/Bonjour - Multicast DNS

#### Description
mDNS (Multicast DNS) est un protocole de résolution de noms sans serveur DNS central. Il permet aux appareils de s'annoncer et de découvrir des services sur le réseau local. **Bonjour** est l'implémentation Apple de mDNS/DNS-SD.

#### Caractéristiques techniques
| Paramètre | Valeur |
|-----------|--------|
| Couche OSI | 7 (Application) |
| Transport | UDP |
| Port | 5353 |
| Adresse multicast IPv4 | 224.0.0.251 |
| Adresse multicast IPv6 | ff02::fb |
| Domaine | `.local` |

#### Fonctionnement dans NetMapper
NetMapper envoie des requêtes DNS-SD (Service Discovery) pour découvrir les services annoncés :

```
┌─────────┐    DNS Query (PTR) to 224.0.0.251:5353    ┌─────────┐
│ Scanner │ ──────────────────────────────────────────▶│ Réseau  │
│         │                                            │ Local   │
│         │◀────────────────────────────────────────── │         │
└─────────┘    DNS Response (PTR, SRV, TXT)           └─────────┘
```

#### Types de services recherchés
| Service | Description | Appareils typiques |
|---------|-------------|-------------------|
| `_http._tcp` | Serveur HTTP | NAS, routeurs, imprimantes |
| `_https._tcp` | Serveur HTTPS sécurisé | NAS, serveurs web |
| `_ssh._tcp` | Accès SSH | Serveurs, Raspberry Pi |
| `_sftp-ssh._tcp` | Transfert SFTP | Serveurs de fichiers |
| `_smb._tcp` | Partage Windows/Samba | NAS, PC Windows |
| `_afpovertcp._tcp` | Apple Filing Protocol | Mac, Time Capsule |
| `_nfs._tcp` | Network File System | Serveurs Unix/Linux |
| `_ftp._tcp` | Transfert FTP | Serveurs FTP |
| `_ipp._tcp` | Internet Printing Protocol | Imprimantes réseau |
| `_printer._tcp` | Imprimante générique | Imprimantes |
| `_airplay._tcp` | Apple AirPlay | Apple TV, HomePod |
| `_raop._tcp` | Remote Audio Output | AirPlay audio |
| `_googlecast._tcp` | Google Cast | Chromecast, Android TV |
| `_spotify-connect._tcp` | Spotify Connect | Enceintes connectées |
| `_homekit._tcp` | Apple HomeKit | Domotique Apple |
| `_hap._tcp` | HomeKit Accessory Protocol | Accessoires HomeKit |
| `_workstation._tcp` | Station de travail | Ordinateurs |
| `_device-info._tcp` | Informations appareil | Divers |

#### Structure d'une requête mDNS
```
┌────────────────────────────────────────┐
│ Transaction ID: 0x0000                 │
│ Flags: Standard Query                  │
│ Questions: 1                           │
│ Answer RRs: 0                          │
│ Authority RRs: 0                       │
│ Additional RRs: 0                      │
├────────────────────────────────────────┤
│ QNAME: _http._tcp.local                │
│ QTYPE: PTR (12)                        │
│ QCLASS: IN (1)                         │
└────────────────────────────────────────┘
```

#### Informations obtenues
- **Services disponibles** : HTTP, SSH, AirPlay, Chromecast, etc.
- **Nom d'hôte** : Nom convivial de l'appareil
- **Port du service** : Via enregistrement SRV
- **Métadonnées** : Via enregistrement TXT

#### Cas d'usage typiques
- Découverte d'Apple TV et HomePod (AirPlay)
- Détection de Chromecast et Android TV (Google Cast)
- Identification d'imprimantes réseau (IPP)
- Localisation de NAS Synology/QNAP (SMB, AFP)

---

### UPnP/SSDP - Universal Plug and Play

#### Description
UPnP (Universal Plug and Play) est un ensemble de protocoles permettant aux appareils de se découvrir et d'interagir automatiquement. **SSDP** (Simple Service Discovery Protocol) est le protocole de découverte utilisé par UPnP.

#### Caractéristiques techniques
| Paramètre | Valeur |
|-----------|--------|
| Couche OSI | 7 (Application) |
| Transport | UDP (découverte), TCP (contrôle) |
| Port | 1900 |
| Adresse multicast IPv4 | 239.255.255.250 |
| Adresse multicast IPv6 | ff02::c, ff05::c |

#### Fonctionnement dans NetMapper
NetMapper envoie une requête M-SEARCH pour découvrir les appareils UPnP :

```
M-SEARCH * HTTP/1.1
HOST: 239.255.255.250:1900
MAN: "ssdp:discover"
MX: 2
ST: ssdp:all

```

#### Réponse SSDP typique
```
HTTP/1.1 200 OK
CACHE-CONTROL: max-age=1800
ST: urn:schemas-upnp-org:device:MediaServer:1
USN: uuid:12345678-1234-1234-1234-123456789abc::...
LOCATION: http://192.168.1.100:8080/description.xml
SERVER: Linux/4.4 UPnP/1.0 MiniDLNA/1.2.1
```

#### Types d'appareils UPnP
| Type | Description | Exemples |
|------|-------------|----------|
| `InternetGatewayDevice` | Routeur/Box internet | Freebox, Livebox |
| `MediaServer` | Serveur multimédia DLNA | NAS, Plex, Kodi |
| `MediaRenderer` | Lecteur multimédia | TV connectée, Sonos |
| `WANDevice` | Connexion WAN | Modems |
| `WANConnectionDevice` | Gestion connexion | NAT, Firewall |
| `Basic` | Appareil basique | Divers IoT |

#### Informations obtenues
- **Type d'appareil** : MediaServer, Gateway, etc.
- **Fabricant et modèle** : Via header SERVER
- **UUID** : Identifiant unique de l'appareil
- **URL de description** : Pour plus de détails (XML)

#### Cas d'usage typiques
- Détection de box internet (Freebox, Livebox, SFR Box)
- Découverte de serveurs DLNA/UPnP (Plex, Emby, Jellyfin)
- Identification de téléviseurs connectés
- Localisation d'enceintes Sonos

---

### SNMP - Simple Network Management Protocol

#### Description
SNMP (Simple Network Management Protocol) est un protocole de gestion de réseau permettant de collecter des informations sur les équipements réseau et de les administrer à distance.

#### Caractéristiques techniques
| Paramètre | Valeur |
|-----------|--------|
| Couche OSI | 7 (Application) |
| Transport | UDP |
| Port | 161 (agent), 162 (traps) |
| Versions | v1, v2c, v3 |

#### Versions SNMP
| Version | Authentification | Chiffrement | Usage |
|---------|-----------------|-------------|-------|
| SNMPv1 | Community string | Non | Legacy |
| SNMPv2c | Community string | Non | Courant |
| SNMPv3 | Utilisateur/mot de passe | Oui (optionnel) | Sécurisé |

#### Fonctionnement dans NetMapper
NetMapper utilise SNMPv1 avec les community strings standards :

```
┌─────────┐    SNMP GET Request (UDP:161)    ┌─────────┐
│ Scanner │ ─────────────────────────────────▶│ Agent   │
│         │    Community: "public"            │ SNMP    │
│         │    OID: 1.3.6.1.2.1.1.1.0        │         │
│         │◀───────────────────────────────── │         │
└─────────┘    SNMP GET Response             └─────────┘
```

#### OIDs interrogés
| OID | Nom | Description |
|-----|-----|-------------|
| 1.3.6.1.2.1.1.1.0 | sysDescr | Description du système |
| 1.3.6.1.2.1.1.5.0 | sysName | Nom du système |
| 1.3.6.1.2.1.1.3.0 | sysUpTime | Temps de fonctionnement |
| 1.3.6.1.2.1.1.4.0 | sysContact | Contact administrateur |
| 1.3.6.1.2.1.1.6.0 | sysLocation | Emplacement physique |

#### Structure d'un paquet SNMP
```
┌─────────────────────────────────────────────┐
│ SEQUENCE (Message)                          │
│ ├─ INTEGER: Version (0 = SNMPv1)           │
│ ├─ OCTET STRING: Community ("public")      │
│ └─ GetRequest-PDU (0xA0)                   │
│    ├─ INTEGER: Request ID                   │
│    ├─ INTEGER: Error Status (0)             │
│    ├─ INTEGER: Error Index (0)              │
│    └─ SEQUENCE: Variable Bindings           │
│       └─ SEQUENCE: VarBind                  │
│          ├─ OID: 1.3.6.1.2.1.1.5.0         │
│          └─ NULL                            │
└─────────────────────────────────────────────┘
```

#### Community strings testées
| Community | Description |
|-----------|-------------|
| `public` | Lecture seule (standard) |
| `private` | Lecture/écriture (standard) |

#### Informations obtenues
- **Nom du système** : Hostname configuré
- **Description** : OS, version, fabricant
- **Uptime** : Temps depuis le dernier redémarrage
- **Contact/Location** : Métadonnées administratives

#### Cas d'usage typiques
- Identification de routeurs et switches managés
- Collecte d'informations sur les imprimantes réseau
- Détection de NAS et serveurs
- Inventaire d'équipements réseau

#### Limitations
- Nécessite que SNMP soit activé sur l'appareil
- Community string doit être connue (souvent "public")
- SNMPv1/v2c transmettent en clair (non sécurisé)

---

## Protocoles de scan

### TCP Connect Scan

#### Description
Le scan TCP Connect établit une connexion TCP complète (three-way handshake) pour déterminer si un port est ouvert.

#### Caractéristiques techniques
| Paramètre | Valeur |
|-----------|--------|
| Couche OSI | 4 (Transport) |
| Protocole | TCP |
| Timeout | 500ms (configurable) |

#### Fonctionnement
```
Port ouvert:
┌─────────┐    SYN           ┌─────────┐
│ Scanner │ ────────────────▶│  Hôte   │
│         │◀──────────────── │         │
│         │    SYN-ACK       │         │
│         │ ────────────────▶│         │
│         │    ACK           │         │
│         │◀──────────────── │         │
└─────────┘    (connexion)   └─────────┘

Port fermé:
┌─────────┐    SYN           ┌─────────┐
│ Scanner │ ────────────────▶│  Hôte   │
│         │◀──────────────── │         │
└─────────┘    RST           └─────────┘
```

#### Ports scannés par profil

**Profil Ping (0 ports)**
- Utilise uniquement ICMP/ARP
- Le plus rapide

**Profil Quick (6 ports)**
| Port | Service | Description |
|------|---------|-------------|
| 22 | SSH | Accès shell sécurisé |
| 80 | HTTP | Serveur web |
| 443 | HTTPS | Serveur web sécurisé |
| 445 | SMB | Partage de fichiers Windows |
| 3389 | RDP | Bureau à distance Windows |
| 8080 | HTTP-Alt | Serveur web alternatif |

**Profil Full (23 ports)**
| Port | Service | Description |
|------|---------|-------------|
| 21 | FTP | Transfert de fichiers |
| 22 | SSH | Shell sécurisé |
| 23 | Telnet | Shell non sécurisé |
| 25 | SMTP | Envoi d'emails |
| 53 | DNS | Résolution de noms |
| 80 | HTTP | Web |
| 110 | POP3 | Réception d'emails |
| 139 | NetBIOS | Services Windows |
| 143 | IMAP | Emails IMAP |
| 443 | HTTPS | Web sécurisé |
| 445 | SMB | Partage Windows |
| 993 | IMAPS | IMAP sécurisé |
| 995 | POP3S | POP3 sécurisé |
| 1433 | MSSQL | SQL Server |
| 3306 | MySQL | Base de données MySQL |
| 3389 | RDP | Bureau à distance |
| 5432 | PostgreSQL | Base de données PostgreSQL |
| 5900 | VNC | Bureau à distance VNC |
| 6379 | Redis | Cache Redis |
| 8080 | HTTP-Alt | Web alternatif |
| 8443 | HTTPS-Alt | HTTPS alternatif |
| 8888 | HTTP-Proxy | Proxy HTTP |
| 27017 | MongoDB | Base de données MongoDB |

---

### Banner Grabbing

#### Description
Le Banner Grabbing consiste à récupérer la bannière d'identification envoyée par un service lors de la connexion.

#### Protocoles supportés
| Protocole | Port | Méthode |
|-----------|------|---------|
| HTTP | 80, 8080, 8888 | Requête GET / |
| SSH | 22 | Lecture passive |
| FTP | 21 | Lecture passive |
| SMTP | 25 | Lecture passive |

#### Exemple de bannières
```
HTTP:
  Server: nginx/1.18.0
  Server: Apache/2.4.41 (Ubuntu)
  Server: Microsoft-IIS/10.0

SSH:
  SSH-2.0-OpenSSH_8.2p1 Ubuntu-4ubuntu0.5
  SSH-2.0-OpenSSH_7.9p1 Raspbian-10+deb10u2

FTP:
  220 ProFTPD 1.3.6 Server ready.
  220 vsftpd 3.0.3

SMTP:
  220 mail.example.com ESMTP Postfix
```

#### Informations obtenues
- **Logiciel serveur** : nginx, Apache, OpenSSH, etc.
- **Version** : Numéro de version précis
- **Système d'exploitation** : Souvent inclus dans la bannière

---

## Protocoles applicatifs

### Wake-on-LAN

#### Description
Wake-on-LAN (WoL) permet de démarrer un ordinateur à distance en envoyant un "magic packet" sur le réseau.

#### Caractéristiques techniques
| Paramètre | Valeur |
|-----------|--------|
| Transport | UDP |
| Port | 9 (standard), 7 (alternatif) |
| Destination | Broadcast (x.x.x.255) |

#### Structure du Magic Packet
```
┌────────────────────────────────────────────────────┐
│ 6 octets de 0xFF (synchronisation)                 │
│ FF FF FF FF FF FF                                  │
├────────────────────────────────────────────────────┤
│ 16 répétitions de l'adresse MAC cible              │
│ AA BB CC DD EE FF (x16)                            │
└────────────────────────────────────────────────────┘
Taille totale: 6 + (6 × 16) = 102 octets
```

#### Prérequis
- L'appareil doit supporter Wake-on-LAN
- WoL doit être activé dans le BIOS/UEFI
- Le pilote réseau doit être configuré pour WoL
- L'appareil doit être connecté en Ethernet (ou WiFi avec support WoWLAN)

---

### SSL/TLS

#### Description
NetMapper extrait les informations des certificats SSL/TLS pour identifier les services HTTPS.

#### Caractéristiques techniques
| Paramètre | Valeur |
|-----------|--------|
| Ports scannés | 443, 8443 |
| Versions supportées | TLS 1.0 - 1.3 |

#### Informations extraites
- **Common Name (CN)** : Nom de domaine du certificat
- **Organisation** : Entité propriétaire
- **Validité** : Dates de début et fin
- **Émetteur** : Autorité de certification

---

## Ports standards

### Tableau récapitulatif

| Port | Protocole | Service | Description |
|------|-----------|---------|-------------|
| 21 | TCP | FTP | File Transfer Protocol |
| 22 | TCP | SSH | Secure Shell |
| 23 | TCP | Telnet | Terminal distant (non sécurisé) |
| 25 | TCP | SMTP | Simple Mail Transfer Protocol |
| 53 | TCP/UDP | DNS | Domain Name System |
| 80 | TCP | HTTP | HyperText Transfer Protocol |
| 110 | TCP | POP3 | Post Office Protocol v3 |
| 139 | TCP | NetBIOS | Windows Network Basic I/O |
| 143 | TCP | IMAP | Internet Message Access Protocol |
| 161 | UDP | SNMP | Simple Network Management Protocol |
| 443 | TCP | HTTPS | HTTP Secure (TLS) |
| 445 | TCP | SMB | Server Message Block |
| 993 | TCP | IMAPS | IMAP over TLS |
| 995 | TCP | POP3S | POP3 over TLS |
| 1433 | TCP | MSSQL | Microsoft SQL Server |
| 1900 | UDP | SSDP | Simple Service Discovery Protocol |
| 3306 | TCP | MySQL | Base de données MySQL |
| 3389 | TCP | RDP | Remote Desktop Protocol |
| 5353 | UDP | mDNS | Multicast DNS |
| 5432 | TCP | PostgreSQL | Base de données PostgreSQL |
| 5900 | TCP | VNC | Virtual Network Computing |
| 6379 | TCP | Redis | Base de données Redis |
| 8080 | TCP | HTTP-Alt | HTTP alternatif |
| 8443 | TCP | HTTPS-Alt | HTTPS alternatif |
| 27017 | TCP | MongoDB | Base de données MongoDB |

---

## Sécurité et bonnes pratiques

### Utilisation responsable

NetMapper est un outil de diagnostic réseau. Son utilisation doit respecter ces principes :

1. **Autorisation** : N'utilisez NetMapper que sur des réseaux dont vous êtes propriétaire ou administrateur, ou pour lesquels vous avez une autorisation explicite.

2. **Légalité** : Le scan de réseaux sans autorisation peut être illégal dans votre juridiction.

3. **Éthique** : Ne pas utiliser les informations collectées à des fins malveillantes.

### Informations sensibles

Les protocoles utilisés peuvent révéler des informations sensibles :

| Protocole | Risque | Mitigation |
|-----------|--------|------------|
| SNMP v1/v2c | Community string en clair | Utiliser SNMPv3 |
| mDNS | Exposition des services | Firewall sur port 5353 |
| UPnP | Exposition des appareils IoT | Désactiver UPnP si non nécessaire |
| Banner Grabbing | Version des logiciels | Masquer les bannières |

### Recommandations

- **Réseau local uniquement** : NetMapper est conçu pour les réseaux locaux
- **Pare-feu** : Un pare-feu bien configuré limitera les informations exposées
- **Mises à jour** : Maintenez vos équipements à jour pour corriger les vulnérabilités
- **Segmentation** : Séparez les appareils IoT du réseau principal

---

## Références

- [RFC 826 - ARP](https://tools.ietf.org/html/rfc826)
- [RFC 792 - ICMP](https://tools.ietf.org/html/rfc792)
- [RFC 6762 - mDNS](https://tools.ietf.org/html/rfc6762)
- [RFC 6763 - DNS-SD](https://tools.ietf.org/html/rfc6763)
- [UPnP Device Architecture](http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1.pdf)
- [RFC 1157 - SNMPv1](https://tools.ietf.org/html/rfc1157)
- [RFC 3411 - SNMPv3](https://tools.ietf.org/html/rfc3411)

---

*Documentation générée pour NetMapper v1.2.0*

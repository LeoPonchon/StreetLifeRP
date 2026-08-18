# StreetLifeRP

Plugin serveur Minecraft orienté **roleplay**, développé en Java pour l'API Spigot 1.21.

Le projet centralise plusieurs systèmes RP : personnages, banque et facturation, chat contextuel, métiers/services, appels d'urgence, menottes et protections anti-abus.

## Fonctionnalités

Le code du dépôt contient notamment des modules pour :

- création et gestion de personnages ;
- banque / économie RP ;
- facturation ;
- chat de proximité et commandes RP ;
- SMS, `/me`, `/do`, OOC et tweets ;
- appels 911 / dispatch ;
- rôles police et EMS ;
- menottes ;
- items et métiers administrables ;
- protections anti-abus ;
- intégrations prévues avec d'autres plugins via l'ordre de chargement.

## Prérequis

- Java **21**
- Maven
- serveur compatible Spigot API **1.21**
- ProtocolLib 5.4 recommandé pour les fonctions qui l'utilisent

Le `plugin.yml` déclare également des intégrations/ordres de chargement autour de Nexo et QualityArmory.

## Build

```bash
git clone https://github.com/LeoPonchon/StreetLifeRP.git
cd StreetLifeRP
mvn clean package
```

Le JAR généré se trouve dans :

```text
target/
```

Copiez-le dans le dossier `plugins/` de votre serveur puis redémarrez ou rechargez l'environnement de test.

> Le `pom.xml` contient actuellement une copie post-build vers un chemin Windows local au développeur. Cette étape est tolérante aux erreurs et n'est pas nécessaire pour produire le JAR.

## Commande principale

```text
/slrp
```

Alias :

```text
/streetliferp
```

## Permissions

Le projet distingue des permissions d'administration et des permissions RP, notamment pour :

- reload ;
- gestion de personnages ;
- items/métiers ;
- police et EMS ;
- réception du dispatch ;
- SMS et commandes de chat RP ;
- appels d'urgence.

Consultez `src/main/resources/plugin.yml` pour la liste exacte.

## Documentation complémentaire

Le dépôt contient aussi des notes dédiées à certains systèmes, notamment les menottes, le chat/input et l'intégration d'armes.

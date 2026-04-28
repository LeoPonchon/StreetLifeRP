# Plan Gunsmith, Gunshell et Oraxen

## Objectif

Mettre en place un écosystème propre pour les armes RP :

- Le métier `GUNSMITH` vend ou fabrique des armes.
- Les armes viennent de `Gunshell-v1.7.3`.
- `Oraxen` centralise le resource pack serveur.
- Les assets de `QAV2` et, si nécessaire, ceux de `Gunshell` sont regroupés dans Oraxen.

## 1. Vérifier les plugins

- Installer ou valider :
  - `Gunshell-v1.7.3`
  - `Oraxen`
  - `QualityArmoryVehicles2`
- Identifier comment Gunshell donne ses armes :
  - commande serveur
  - item custom
  - API Java
  - fichiers de config
- Vérifier si Gunshell fournit déjà :
  - textures
  - modèles 3D
  - sons
  - resource pack séparé

## 2. Centraliser les resource packs

- Utiliser Oraxen comme source unique du resource pack.
- Importer ou reproduire les assets QAV2 dans Oraxen.
- Importer les assets Gunshell dans Oraxen si Gunshell ne les gère pas déjà proprement.
- Désactiver les packs séparés QAV2/Gunshell si nécessaire.
- Tester côté client :
  - véhicules QAV2 visibles
  - armes Gunshell visibles
  - sons fonctionnels
  - aucun conflit de `CustomModelData`

## 3. Boutique armurier

- Ajouter une interface métier `GUNSMITH`.
- Lister les armes vendables en config.
- Pour chaque arme :
  - nom affiché
  - identifiant Gunshell
  - prix
  - slot UI
  - permission ou métier requis
- Donner l’arme via la méthode la plus fiable :
  - API Gunshell si disponible
  - commande console sinon
- Ajouter :
  - anti-abuse achat
  - logs audit
  - refus si métier incorrect
  - refus si fonds insuffisants

## 4. Crafts armurier

- Garder la recette `Boîte de munitions` pour `GUNSMITH`.
- Ajouter éventuellement des composants :
  - canon
  - culasse
  - ressort
  - chargeur
  - poignée
- Décider le modèle économique :
  - armes achetées/revendues via boutique
  - armes craftées avec composants
  - mix des deux

## 5. Métiers RP à compléter

- `BAKER` :
  - nourriture
  - boissons
  - buffs légers
- `DEALER` :
  - drogues de synthèse
  - effets potion
  - risques ou malus
- `MECHANIC` :
  - kits de réparation
  - composants véhicules
- `GUNSMITH` :
  - armes Gunshell
  - munitions
  - composants d’armes
- `POLICE` :
  - équipements encadrés
  - interactions justice
- `EMS` :
  - soins
  - médkits
  - défibrillateurs

## 6. Tests serveur

- Démarrer le serveur sans erreur console.
- Vérifier que les crafts vanilla sont bloqués sans spam d’erreurs.
- Tester les crafts métier avec le bon job et le mauvais job.
- Tester la boutique armurier avec et sans métier `GUNSMITH`.
- Tester les armes Gunshell :
  - affichage
  - tir
  - reload
  - dégâts
  - sons
- Tester les véhicules QAV2 après migration vers Oraxen.

## Notes

- Ne pas supprimer les recettes `minecraft:*` du registre serveur : ça casse le livre de recettes et provoque des erreurs Paper.
- Préférer bloquer le résultat des crafts vanilla via listeners Bukkit.
- Garder Oraxen comme autorité unique pour éviter les conflits de resource pack.

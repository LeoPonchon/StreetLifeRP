# StreetLifeRP — périmètre exact (Minecraft/Spigot)

## Interactions (état actuel)
- Blocs : pas d’interaction (portes, leviers, coffres, etc).
- Joueurs : interaction OK (combat, tir à l’arc, attaques, etc).

## RP / Chat
- Chat de proximité (si `chat.proximity.enabled: true`) : annule le chat vanilla et envoie aux joueurs dans un rayon `chat.proximity.radius`.
- Téléphone (par défaut) : slot 9 réservé (item “Téléphone”), non remplaçable, donné à la connexion ; clic droit -> menu d’applications (SMS, 911, boutique, job, portefeuille, identité, cash).

## Personnage / Identité
- Création personnage : via le téléphone (saisie via chat).
- Carte d’identité : via le téléphone (donne un item `PAPER` avec les infos).

## Économie
- Cash + banque par joueur (stockés en YML).
- Cash (item) : “billet” (`PAPER`) avec une valeur, clic droit -> encaisse dans le cash du joueur.
- Trade (paiement) : shift + clic droit main vide sur un joueur -> interface de trade (items Cash uniquement, confirmations + délai 3s).

## Boutique
- Boutique via l’app Téléphone : ouvre une GUI (27 slots) configurée via `shop.*` (items + prix).
- Clic dans la GUI : achète en débitant le cash, puis donne l’item (refuse si anti-abuse ou fonds insuffisants).

## Jobs
- Types : `UNEMPLOYED`, `DELIVERY`, `MECHANIC`, `POLICE`, `EMS`.
- Job via l’app Téléphone : paie un salaire défini dans `jobs.<job>.salary` avec cooldown `jobs.<job>.cooldown_seconds`.

## Justice / Police / EMS
- Menottes : clic droit sur un joueur avec l’item `HANDCUFFS` -> toggle menottes (interdit l’usage d’objets).
- Amende : (police) clic droit sur un joueur avec la main vide -> menu d’amende (montants presets / perso).

## Véhicules
- Non couvert par ce plugin : géré entièrement par QualityArmory Vehicles 2 (ex: `/qav spawnVehicle <vehicle_name>`).

## Données / Config / Logs
- Données joueur : `plugins/StreetLifeRP/players/<uuid>.yml` (chargées au join, sauvegardées au quit + à l’arrêt).
- Audit log : `plugins/StreetLifeRP/logs/audit.log` (événements “SENSITIVE” : banque, transferts, achats, justice, véhicules…).
- Aucune commande (mode full interactions).

## Anti-abuse (cooldowns)
- Cooldowns par action dans `antiabuse.cooldowns_seconds.*` (argent, banque, shop, job, cash redeem, 911, SMS).

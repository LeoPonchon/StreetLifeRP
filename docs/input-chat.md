# Saisies via chat (InputService)

Certaines fonctionnalites demandent au joueur d'ecrire une reponse dans le chat (ex: SMS, creation de personnage).

Comportement attendu :
- Si une saisie est en attente pour le joueur, son prochain message **n'est pas envoye** au chat normal.
- Le message est route vers le handler de saisie (`InputService`).

Implementation :
- Interception du chat : `src/main/java/org/shimakuro/streetLifeRP/input/InputListener.java`
- Stockage/dispatch de la saisie : `src/main/java/org/shimakuro/streetLifeRP/input/InputService.java`


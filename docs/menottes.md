# Menottes (cuffed)

Quand un joueur est **menotte** (`justice.cuffed = true`), le plugin bloque ses interactions principales avec le monde :

- Interactions (air/blocs/entites), y compris avec la main secondaire (off-hand)
- Casse / pose de blocs
- Deposer des items (drop)
- Manipulation d'inventaire (clic + drag)
- Degats a une entite (attaque melee, etc.)
- Tir / lancement de projectiles (arc, arbalete, trident, etc.)

Implementation : `src/main/java/org/shimakuro/streetLifeRP/justice/CuffedRestrictionListener.java`

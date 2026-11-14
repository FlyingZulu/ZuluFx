# Cle de signature Zulu

Cle fixe du projet (PKCS12). Elle signe TOUS les APK (debug et release),
pour que les mises a jour s'installent sans desinstaller l'ancienne version.

- Fichier : signing/zulu.p12
- Alias   : zulu
- Mot de passe : zuluzulu2000

Le depot etant public, la cle y est visible : c'est un choix assume
pour une application distribuee en sideload (pas de Play Store).
NE JAMAIS supprimer ni regenerer ce fichier : les mises a jour
seraient refusees par Android.

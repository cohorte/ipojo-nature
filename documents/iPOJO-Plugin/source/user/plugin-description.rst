
Description du plugin
=====================

Ce projet est un plugin pour Eclipse Helios permettant d'utiliser facilement 
iPOJO dans cet environnement de développement.
Il fournit une nouvelle nature de projet et un "builder" associé à cette nature.

Le but n'est pas (actuellement) d'assister l'utilisateur dans la création des
fichiers de description iPOJO (metadata.xml, annotations, ...) mais de lui 
permettre d'utiliser des bundles iPOJO dans ses configurations d'exécution sans 
avoir à passer par Maven ou par un fichier JAR à placer dans une 
"Target Platform" de test.


Outils existants
================

Nous avons trouvé deux outils concernant l'intégration d'iPOJO dans Eclipse :

* Le plugin Eclipse fourni par Apache Felix
  Site Web: `<http://felix.apache.org/site/ipojo-eclipse-plug-in.html>`_
  Il permet d'exporter le projet sous forme d'un JAR traité par iPOJO.
  Géré par Clément ESCOFFIER, il ne semble pas avoir évolué depuis 2008.

* Le builder iPOJO du projet CADSE du laboratoire ADELE (Grenoble)
  Site Web : `<http://code.google.com/a/eclipselabs.org/p/cadse/>`_
  Découvert sur le tard, il s'agit d'un projet équivalent au notre.
  Géré par Stéphane CHOMAT, la dernière mise à jour date de Septembre 2010.


Principe de fonctionnement
==========================

Le principe du plugin est d'ajouter la nature "iPOJO" à un projet existant, 
ajoutant notre builder à sa liste.
Ce plugin ajoute également une entrée "Update Manifest" dans le menu contextuel 
des fichiers Manifest.mf, permettant de faire une "Pojoization" manuelle.

Lorsque le builder est appelé, ou qu'une mise à jour manuelle est demandée, le 
plugin demande une compilation complète du projet au plugin JDT, puis utilise 
l'outil iPOJO Manipulator pour effectuer le traitement des fichiers .class 
générés et du fichier Manifest.
Il ne s'agit pour le moment que d'une recherche des fichiers dans le projet et 
le format Eclipse pour les transmettre au Manipulator, utilisant les interfaces 
Java standards.

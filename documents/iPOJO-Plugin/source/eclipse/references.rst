.. Document references for Eclipse builder plugin creation

Références de développement
===========================

Intéractions avec le workspace
------------------------------

Modifications de ressources :

* `Suivi des modifications de ressources <http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/resAdv_events.htm>`_
* `Traitement des modifications de ressources <http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/resAdv_batching.htm>`_
* `Hook sur les modifications de ressources <http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/resAdv_hooks.htm>`_
* `Ressource "dérivées" <http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/resAdv_derived.htm>`_


Gestion de projet :

* `Nature de projet <http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/resAdv_natures.htm>`_


Builder :

* `Builder incrémental <http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/resAdv_builders.htm>`_


Intéractions avec JDT
---------------------

Quelques références pouvant être utiles :

* `Exécution d'un programme Java <http://help.eclipse.org/helios/index.jsp?topic=/org.eclipse.jdt.doc.isv/guide/jdt_api_run.htm>`_

Builder Eclipse
---------------

Nature de projet
^^^^^^^^^^^^^^^^

Un builder est associé à un projet en passant par la notion de *nature*.
Cette notion permet également d'associer un projet à des plugins.

L'association d'un builder à un projet se fait à l'aide de la méthode
*configure()*.

+-----------------------+-----------------------------------------+
| **Point d'extension** | org.eclipse.core.resources.natures      |
+-----------------------+-----------------------------------------+
| **Interface**         | IProjectNature                          |
+-----------------------+-----------------------------------------+
| **Références**        | Page 123-136 du diaporama de M. Baron ; |
+-----------------------+-----------------------------------------+


Builder
^^^^^^^

Le compilateur en lui-même.
Il existe un modèle fourni par Eclipse sur lequel on peut s'inspirer.

+-----------------------+-----------------------------------------+
| **Point d'extension** | org.eclipse.core.resources.builders     |
+-----------------------+-----------------------------------------+
| **Interface**         | IncrementalProjectBuilder (classe)      |
+-----------------------+-----------------------------------------+
| **Références**        | Page 108-122 du diaporama de M. Baron ; |
+-----------------------+-----------------------------------------+


Éditeur de fichier metadata.xml
-------------------------------

Voir les références utilisées pour le plugin **ReST Editor**


Liens utiles pour iPOJO
-----------------------

* `Principe de la manipulation <http://felix.apache.org/site/dive-into-the-ipojo-manipulation-depths.html>`_

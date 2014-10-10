(ns reclojure.lang.protocols.editable-collection)

(defprotocol EditableCollection
  (as-transient [this]))

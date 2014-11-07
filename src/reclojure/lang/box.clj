(ns reclojure.lang.box)

(definterface Holder
  (update [v]))

(deftype Box [^:unsynchronized-mutable value]
  clojure.lang.IDeref
  (deref [this] value)

  Holder
  (update [this v] (set! value v)))

  ;;ITransientMap doAssoc(Object key, Object val) {
  ;;  if (key == null) {
  ;;    if (this.nullValue != val)
  ;;      this.nullValue = val;
  ;;    if (!hasNull) {
  ;;      this.count++;
  ;;      this.hasNull = true;
  ;;    }
  ;;    return this;
  ;;  }
;;    Box leafFlag = new Box(null);
  ;;  leafFlag.val = null;
  ;;  INode n = (root == null ? BitmapIndexedNode.EMPTY : root)
  ;;    .assoc(edit, 0, hash(key), key, val, leafFlag);
  ;;  if (n != this.root)
  ;;    this.root = n;
  ;;  if(leafFlag.val != null) this.count++;
  ;;  return this;
  ;;}

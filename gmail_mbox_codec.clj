(ns gmail-mbox-codec
  (:require [clojure.java.io :as io])
  (:import [java.io File BufferedReader BufferedWriter]
           [java.nio.file StandardCopyOption Files Paths]
           [java.nio.file.attribute FileAttribute]
           [java.util HexFormat]
           [java.security MessageDigest DigestOutputStream]))

(def from-pattern #"From \d+@xxx \w{3} \w{3} \d{2} \d{2}:\d{2}:\d{2} \+\d{4} \d{4}")
(def hex-format (HexFormat/of))
(def sequence-file-name "sequence")

(defn chunk-path [digest]
  (Paths/get "chunks" (into-array [(subs digest 0 2)
                                   (subs digest 2 4)
                                   (subs digest 4)])))

(defn rename [from to]
  (Files/move from to (into-array [StandardCopyOption/REPLACE_EXISTING])))

(defn temp-file []
  (File/createTempFile "gmail-mbox-codec-" ".tmp" (File. ".")))

(defn split []
  (let [reader (io/reader System/in)
        sequence-file (temp-file)]
    (with-open [sequence-writer (io/writer sequence-file)]
      (loop [chunk-file nil
             chunk-writer nil
             chunk-digester nil]
        (let [line (.readLine ^BufferedReader reader)
              from? (boolean (when line (re-matches from-pattern line)))]
          (when (and chunk-file (or from? (nil? line)))
            ;; chunk is finished, file it away
            (.close chunk-writer)
            (let [digest (.formatHex hex-format (.digest chunk-digester))
                  file (chunk-path digest)
                  dir (.getParent file)]
              (Files/createDirectories dir (into-array FileAttribute []))
              (rename (.toPath chunk-file) file)
              (.append sequence-writer (str file))
              (.newLine sequence-writer)))
          (when line
            (if from?
              ;; new chunk starting
              (let [file (temp-file)
                    digester (MessageDigest/getInstance "md5")
                    writer (io/writer (DigestOutputStream. (io/output-stream file) digester))]
                (.write writer line)
                (.write writer "\r\n")
                (recur file writer digester))
              (do
                ;; just copy the line
                (.write ^BufferedWriter chunk-writer line)
                (.write ^BufferedWriter chunk-writer "\r\n")
                (recur chunk-file chunk-writer chunk-digester)))))))
    (rename (.toPath sequence-file) (.toPath (File. sequence-file-name)))))

(defn join []
  (with-open [sequence-reader (io/reader sequence-file-name)]
    (doseq [chunk-file (line-seq sequence-reader)]
      (with-open [chunk-reader (io/input-stream chunk-file)]
        (.transferTo chunk-reader System/out)))))

(case (first *command-line-args*)
  "split" (split)
  "join" (join)
  (do
    (.println System/out "Usage: clojure -M gmail-mbox-codec.clj (split|join)")
    (System/exit 1)))

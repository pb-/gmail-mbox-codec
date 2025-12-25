#!/usr/bin/env bash

set -exuo pipefail

clojure -M ../gmail_mbox_codec.clj split < sample.mbox
clojure -M ../gmail_mbox_codec.clj join > restored.mbox

md5sum -c checksums

rm sequence restored.mbox
rm -rf chunks

echo "Test passed"

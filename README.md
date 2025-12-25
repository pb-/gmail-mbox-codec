# gmail-mbox-codec

A tiny script to enable efficient incremental file-based backups from Gmail takeouts. The script partitions the takeout's mbox file deterministically into chunks that _approximately_ correspond to individual mails, thereby allowing a file-based backup solution to operate in an incremental manner.

For a detailed discussion of the problem, please have a look at [this companioning article](https://baecher.dev/stdout/incremental-backups-of-gmail-takeouts/).


## Usage

The script is written in Clojure, so you'll need the Clojure runtime.

To split your `mbox` file into file chunks (stored in the current directory):

```sh
clojure -M gmail_mbox_codec.clj split < 'All mail Including Spam and Trash.mbox'
```

To restore the original `mbox` file from the chunks:

```sh
clojure -M gmail_mbox_codec.clj join > restored.mbox
```

Verify round-trip correctness:

```sh
$ md5sum 'All mail Including Spam and Trash.mbox' restored.mbox
f87e6ed7682cff4e95c8d96a833a0d80  All mail Including Spam and Trash.mbox
f87e6ed7682cff4e95c8d96a833a0d80  restored.mbox
```

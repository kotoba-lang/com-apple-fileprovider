# com-apple-fileprovider

**Design only, and the only one of the four that cannot be written in
Clojure at all.** That is the finding; the rest of this file is what it
would take.

Apple's File Provider framework is what Google Drive, Dropbox and iCloud
Drive use today. It is the only way to get the behaviour people mean when
they say "it shows up like Google Drive":

- an entry in the Finder sidebar that survives reboot,
- **online-only placeholder files** that materialise on open,
- per-file sync badges and progress,
- eviction of local copies under disk pressure.

`org-ietf-nfs` gives a mounted volume. It does not give any of the four.

## What is in the way

A File Provider is a **macOS app extension**: Swift or Objective-C,
subclassing `NSFileProviderReplicatedExtension`, bundled inside a signed
host application, with the `com.apple.developer.fileprovider.*` entitlement.
There is no protocol to speak and no port to bind — the OS loads your code.

That collides head-on with this workspace's rule against writing new native
implementations (ADR-2607072000, and the runtime priority in CLAUDE.md).
**Adopting it needs an explicit exception ADR first**, naming the scope, the
review boundary and the removal condition. The signing infrastructure itself
already exists (see the `secrets-location-map` skill, mobile-publishing).

## The shape it would take

```
com-apple-fileprovider
  Swift extension           NSFileProviderReplicatedExtension — the only native part
    ↕ XPC / local HTTP
  cloud-itonami-app         enumeration, materialisation, upload
    nfs.v3/IFilesystem      the same injected filesystem every other surface uses
```

The native part stays a **transport shim with no decisions in it** — the
same line `nfs.tcp` draws, and the same one ADR-2607241100 draws for
decision-free C. Enumeration, conflict handling and materialisation policy
belong on the Clojure side where they can be tested.

## The condition for starting

Someone wants online-only files and a permanent sidebar entry badly enough
to accept a signed native extension. Until then, a mounted NFS volume is
the honest answer to "can it look like a disk".

## License

Apache-2.0.

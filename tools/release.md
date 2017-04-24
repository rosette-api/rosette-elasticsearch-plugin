Releasing the Elasticsearch plugin
==================================

## Requirements

To release you need:
- an OSSRH account
- gpg installed and a pgp key published
- credentials for the above added to your maven settings file

The process for this is described [here](https://git.basistech.net/raas/rosapi1.5/blob/master/doc/release-binding.md#request-access-to-ossrh-if-not-already-done).

## Releasing

A single script takes care of running the maven release process as well as creating the release in github.

From the root directory of the repo run:

`./tools/release.sh git_username=<github.com username> rel_notes=<release notes file>`

As an example:
`./tools/release.sh git_username=bwsawyer rel_notes=docs/rel-notes.md`

If successful head to https://oss.sonatype.org/#stagingRepositories to release the staged artifacts.
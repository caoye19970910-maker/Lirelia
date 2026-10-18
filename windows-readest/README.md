# Lirelia Readest for Windows

This folder adds Lirelia language learning to the complete Readest v0.12.16 application.
The Windows workflow checks out the exact upstream commit, applies `upstream.patch`,
copies `overlay/`, then builds the original Tauri desktop application with its local
Python learning sidecar. Existing Android source remains on its original branch.

The output artifact is `Lirelia-Readest-Windows-x64-installer` and contains the NSIS EXE.
Do not put API keys or user databases in this repository. Keys are entered in the
installed application and retained in the user's Windows Credential Manager.

Upstream Readest: https://github.com/readest/readest
Commit: `3cc39ad7a558e6612e1a8135058fbd6f3c90b3e3`
Readest and its components retain their original licenses. See the complete source
and license files checked out by the workflow.

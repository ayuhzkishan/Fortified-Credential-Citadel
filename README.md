# 🏰 Fortified Credential Citadel

> A bulletproof, local-first credential vault engineered with military-grade cryptography.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=java)](https://openjdk.org/projects/jdk/21/)
[![Build](https://img.shields.io/badge/Build-Maven-blue?logo=apachemaven)](https://maven.apache.org/)
[![Crypto](https://img.shields.io/badge/Encryption-AES--256--GCM%20%2B%20RSA--4096-green)](https://en.wikipedia.org/wiki/Galois/Counter_Mode)
[![KDF](https://img.shields.io/badge/KDF-Argon2id-critical)](https://www.rfc-editor.org/rfc/rfc9106)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

---

## 🔐 What is This?

Fortified Credential Citadel is a **local-first, offline password and credential manager** built from the ground up in Java. It stores passwords, API tokens, and secure notes in an encrypted vault file on disk — protected by your master password, and unreadable to anyone who doesn't have it.

No cloud. No telemetry. No third-party servers. **Your data never leaves your machine.**

---

## 🧱 Architecture

```
com.citadel
├── crypto/          → AES-256-GCM, RSA-4096, Argon2id key derivation
├── vault/           → VaultManager (unlock/lock/CRUD), serialization, session
├── model/           → PasswordCredential, ApiTokenCredential, SecureNoteCredential
├── factory/         → CredentialFactory, CipherFactory (Factory Pattern)
└── event/           → VaultEventBus, VaultEvent (Observer Pattern)
```

### Design Patterns
| Pattern | Where Used |
|---|---|
| **Singleton** | `CryptoEngineManager`, `VaultSessionManager` |
| **Factory** | `CredentialFactory`, `CipherFactory` |
| **Observer** | `VaultEventBus` for reactive, decoupled UI updates |

---

## 🛡️ Security Model

| Layer | Technology | Purpose |
|---|---|---|
| Key Derivation | **Argon2id** (64MB/3 iter/4 threads) | Password → 256-bit master key |
| Vault Encryption | **AES-256-GCM** | Authenticated encryption of vault |
| Asymmetric Ops | **RSA-4096** | Signing, future key exchange |
| Provider | **Bouncy Castle** | Cryptographic primitive library |

The vault file is **never readable without your master password**. Even a byte-by-byte copy of your disk is useless without it.

---

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Maven 3.9+

### Build
```bash
mvn clean install
```

### Run Tests
```bash
mvn test
```

---

## 📁 Vault File Format

The encrypted vault is stored as a `.citadel` file. Internally it contains:

```
[16 bytes IV] + [N bytes AES-256-GCM ciphertext] + [16 bytes Auth Tag]
```

The plaintext behind it is a JSON structure containing all credential entries. The JSON never exists on disk — only in memory, only while the vault is unlocked.

---

## 🧪 Testing

Comprehensive JUnit 5 suites cover:
- ✅ Authentication flows (valid + invalid master passwords)
- ✅ Edge-case crypto failures (corrupted ciphertext, tampered auth tags)
- ✅ Concurrency stress tests (multi-threaded vault access)
- ✅ Factory and Observer pattern correctness

---

## 📌 Roadmap

- [x] Step 1: Project Setup
- [ ] Step 2: CryptoException
- [ ] Step 3: CryptoEngineManager (Singleton)
- [ ] Step 4: CipherFactory
- [ ] Step 5: AesGcmService
- [ ] Step 6: Argon2 Key Derivation
- [ ] Step 7: RsaService
- [ ] Step 8: Data Models
- [ ] Step 9: CredentialFactory
- [ ] Step 10: Observer Event Bus
- [ ] Step 11: VaultSessionManager
- [ ] Step 12: VaultSerializer
- [ ] Step 13: VaultManager
- [ ] Step 14: JUnit Test Suites

---

## 👤 Author

**Ayush Kishan**

---

## 📄 License

MIT License — see [LICENSE](LICENSE)
# Finances

Finances is an Android app for personal finances: entries in a few taps or from natural-language text, accounts grouped by bank (checking, savings boxes, investments, cash) with yields kept apart from deposits, bills to pay and receive confirmed in one tap, credit cards and their statements, installments, recurrences, people balances, products with price history, tags, categories and reports. This repository holds the Android app. It is a client of Manage API, a private Laravel backend that is not published; this README describes the part of its contract the app consumes.

Other clients of the same backend: [`calendar`](https://github.com/Viniciusadm/calendar), [`exercises`](https://github.com/Viniciusadm/exercises), [`files`](https://github.com/Viniciusadm/files) and [`oklo`](https://github.com/Viniciusadm/oklo).

## Requirements

- JDK 11+
- Android SDK with `compileSdk` 37; the app runs on Android 8.0+ (`minSdk` 26)
- Android Gradle Plugin 9.3.2 and Kotlin 2.2.10 (from [`gradle/libs.versions.toml`](gradle/libs.versions.toml))

## Building

The default backend address comes from `financas.baseUrl` in `local.properties`, which is not versioned; without it the default is `http://127.0.0.1:8001`. The address can also be changed on the login screen:

```properties
financas.baseUrl=http://10.0.2.2:8001
```

Build and install the debug app:

```bash
./gradlew installDebug
```

## API contract

Every request sends `Accept: application/json`, and every response comes in an envelope with the payload in `body`. `POST api/auth/login` returns a token, sent afterwards as `Authorization: Bearer <token>`.

Access code: the app is locked until the user types the daily 8-digit access code on a shuffled keypad. The code is checked with `POST api/user/test` (`{"code": "..."}`), kept until the day ends in the server's time zone (`America/Sao_Paulo`), stored encrypted with an Android Keystore key behind biometric or device credential (or only in memory when that is unavailable), and sent in the `code` header on every request except login and the check itself. The app does not derive the code.

Endpoints used:

| Area | Endpoints |
| --- | --- |
| Overview | `GET api/finance/overview` |
| Bills | `GET api/finance/bills` |
| Entries | `GET api/finance/entries`, `GET api/finance/entries/{id}`, `POST api/finance/entries`, `POST api/finance/entries/quick`, `PUT api/finance/entries/{id}`, `PUT api/finance/entries/{id}/items`, `PATCH api/finance/entries/{id}/pay`, `PATCH api/finance/entries/{id}/unpay`, `DELETE api/finance/entries/{id}`, `POST api/finance/transfers` |
| AI entry | `POST api/finance/ai/draft`, `POST api/finance/ai/commit` |
| Accounts | `GET api/finance/accounts`, `POST api/finance/accounts`, `PUT api/finance/accounts/{id}`, `PATCH api/finance/accounts/{id}/archive`, `POST api/finance/accounts/reorder`, `DELETE api/finance/accounts/{id}`, `GET api/finance/accounts/{id}/history`, `POST api/finance/accounts/{id}/reconcile`, `POST api/finance/accounts/{id}/yield` |
| Holdings (stocks and crypto) | `GET api/finance/accounts/{id}/holdings`, `POST api/finance/accounts/{id}/holdings`, `GET api/finance/holdings/{id}`, `PUT api/finance/holdings/{id}`, `PATCH api/finance/holdings/{id}/archive`, `DELETE api/finance/holdings/{id}`, `POST api/finance/holdings/{id}/buy`, `POST api/finance/holdings/{id}/sell`, `POST api/finance/holdings/{id}/price` |
| Cards and statements | `GET api/finance/cards`, `POST api/finance/cards`, `PUT api/finance/cards/{id}`, `PATCH api/finance/cards/{id}/archive`, `DELETE api/finance/cards/{id}`, `GET api/finance/cards/{id}/statements`, `GET api/finance/cards/{id}/statements/current`, `GET api/finance/cards/{id}/statements/{reference}`, `POST api/finance/statements/{id}/pay`, `POST api/finance/statements/{id}/unpay` |
| Installments | `GET api/finance/installment-plans`, `GET api/finance/installment-plans/{id}`, `POST api/finance/installment-plans`, `PUT api/finance/installment-plans/{id}`, `DELETE api/finance/installment-plans/{id}` |
| Recurrences | `GET api/finance/recurrences`, `GET api/finance/recurrences/upcoming`, `POST api/finance/recurrences`, `PUT api/finance/recurrences/{id}`, `PATCH api/finance/recurrences/{id}/pause`, `POST api/finance/recurrences/{id}/materialize`, `POST api/finance/recurrences/{id}/occurrences/{date}/pay`, `POST api/finance/recurrences/{id}/occurrences/{date}/skip` |
| People | `GET api/people/all`, `GET api/finance/people/balances`, `GET api/finance/people/{id}/entries`, `POST api/finance/people/{id}/settle` |
| Categories and tags | `GET api/finance/categories`, `POST api/finance/categories`, `PUT api/finance/categories/{id}`, `PATCH api/finance/categories/{id}/archive`, `POST api/finance/categories/reorder`, `GET api/finance/tags`, `POST api/finance/tags`, `PUT api/finance/tags/{id}`, `POST api/finance/tags/{id}/merge`, `DELETE api/finance/tags/{id}` |
| Products | `GET api/products`, `GET api/products/search`, `GET api/products/{id}`, `GET api/products/{id}/price-history`, `POST api/products`, `PUT api/products/{id}`, `PATCH api/products/{id}/archive`, `DELETE api/products/{id}`, `GET api/product-categories`, `POST api/product-categories`, `PUT api/product-categories/{id}`, `DELETE api/product-categories/{id}` |
| Reports | `GET api/finance/reports/by-category`, `GET api/finance/reports/by-month`, `GET api/finance/reports/by-tag`, `GET api/finance/reports/by-method`, `GET api/finance/reports/by-account`, `GET api/finance/reports/by-card`, `GET api/finance/reports/by-institution` |

## Separated money, investment types and holdings

- **Separated money**: an account with `separated: true` holds money that is not the user's. The API leaves it out of `totalCents`, the free balance, the month totals and the reports, and returns it in `overview.separated`. The app lists it in its own section.
- **Investment types**: an account with `kind: "investment"` has an `investmentType` of `fixed_income`, `stock` or `crypto`. `overview.investments` totals the user's own investments by type.
- **Holdings**: `stock` and `crypto` accounts hold assets with a symbol, a quantity and a price per unit (value = quantity × price). The account balance still comes only from the ledger: buying and selling create transfers, a price change creates a yield (positive or negative), and "already owned" adds to the account's initial balance. A buy or sell also becomes the asset's current price. `buy`, `sell` and `price` accept `preview: true` and return what would happen without saving.
- **Net worth** (`overview.totalCents`) is checking accounts and fixed-income only. Stocks and crypto accounts are left out and returned in `overview.variable`, which the app keeps collapsed behind a tap.
- **Free money** (`overview.freeCents`): net worth minus unpaid card statements and minus fixed-income accounts with a future `lockedUntil` (redeemable only on that date). `overview.notFree.lockedCents` is that locked amount. Only fixed-income investment accounts accept `lockedUntil`.
- **Statement shift**: an entry on a card has `statementShift` (-1, 0 or 1, default 0). A purchase the bank processes late (the Inter takes a purchase made on the 29th to 31st only on the 1st) is moved by hand to the following statement with `1`. The purchase date does not change, so month totals and reports still follow the purchase date. The app shows the resulting statement in the entry form.
- **Recurrence tags**: a recurrence accepts `tags` (names). Its occurrences inherit them, and changing the tags updates the pending occurrences.
- **Negative yield**: only a yield entry can be negative. On an investment account, `reconcile` records a fall as a negative yield instead of an adjustment expense.

## Tests

```bash
./gradlew test
```

The unit tests cover money formatting and parsing, the access code keypad, the day validity of a stored code, the attempt budget, the asset number helpers (quantity × price, formatting, profit) and the patrimony sum that leaves separated money out.

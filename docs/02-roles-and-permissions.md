# AisleGo — Roles and Permissions

**Related docs:** `01-PRD.md` (role feature lists), `08-security-and-fraud-control.md` (authorization enforcement model)

---

## 1. Access Scope Model

Before the permissions matrix, it's important to understand that AisleGo authorization is not flat — every permission is exercised within a **scope**, and mixing scopes up is the single most common source of authorization bugs in multi-tenant marketplaces. AisleGo defines three scopes:

| Scope | Meaning | Example roles |
|---|---|---|
| **Platform-scoped** | Applies across the entire AisleGo platform, across all supermarkets and customers. | Platform administrator |
| **Store-scoped** | Applies to one supermarket business and all of its branches. | Supermarket owner |
| **Branch-scoped** | Applies to exactly one physical branch of one supermarket. | Branch manager, store employee/picker |
| **Self-scoped** | Applies only to the acting user's own records. | Customer, delivery partner |

Every authorization check in the system answers two questions, not one: *"Can this role perform this action?"* **and** *"Is the target record within this actor's scope?"* A branch manager who is technically allowed to "accept orders" must still be blocked from accepting an order belonging to a different branch, even one in the same supermarket business. This scoping rule is enforced server-side on every request — see `08-security-and-fraud-control.md` §1 and §7 for the implementation approach (JWT claims carrying `supermarket_id`/`branch_id`, and scoped repository queries).

## 2. Roles Summary

| Role | Primary scope | Typical device |
|---|---|---|
| Customer | Self | Mobile PWA |
| Supermarket owner | Store (all branches under their business) | Desktop / tablet dashboard |
| Branch manager | Branch (single) | Tablet / desktop at branch |
| Store employee / picker | Branch (single, task-level) | Handheld / tablet on shop floor |
| Delivery partner | Self | Mobile PWA |
| Platform administrator | Platform | Desktop admin console |

## 3. Permissions Matrix

Legend: **F** = Full access · **S** = Scoped access (own branch/store/self only) · **A** = Approval/escalation only (above a threshold or on referral) · **R** = Read-only · **—** = No access

| Capability | Customer | Supermarket Owner | Branch Manager | Store Employee/Picker | Delivery Partner | Platform Admin |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| Register / manage own profile | S | S | S | S | S | S |
| Browse stores & catalogue | F | — | — | — | — | R |
| View own store's products & prices | — | F | S | R (assigned branch) | — | R |
| Manage products (create/edit/delete) | — | F | S (branch-level pricing/stock only, per policy) | — | — | — |
| Manage categories (global taxonomy) | — | — | — | — | — | F |
| Add products via barcode/CSV import | — | F | S | — | — | — |
| Manage live inventory / stock counts | — | F | S | S (mark picked/OOS only) | — | R |
| Reserve inventory during checkout | (triggers reservation) | R | S | — | — | R |
| Create offers, coupons, bundles | — | F | S (branch-specific offers, if permitted) | — | — | F (platform-wide promotions) |
| Place an order | F | — | — | — | — | — |
| Accept / reject incoming orders | — | R | S | — | — | R |
| Assign order to staff | — | S | S | — | — | — |
| Approve substitutions | S (pre-approve list) | S | S | Suggests only | — | — |
| Mark order ready for pickup | — | R | S | S | — | R |
| Accept / reject delivery assignment | — | — | — | — | S | — |
| Confirm pickup / delivery OTP | — | R | R (own branch) | R | S | R |
| Manage branches (create/edit) | — | F | S (own branch details only) | — | — | F |
| Manage staff accounts & permissions | — | F (own business) | S (pickers at own branch) | — | — | F (all accounts) |
| Process cancellations / refunds | Requests only | S (within policy limit) | S (within policy limit) | — | — | A (above threshold, all disputes) |
| View store-level sales & analytics | — | F | S (own branch) | — | — | F (platform-wide) |
| View platform-wide analytics | — | — | — | — | — | F |
| Configure commissions / subscriptions | — | R (own terms) | — | — | — | F |
| Verify supermarkets / delivery partners | — | — | — | — | — | F |
| Suspend accounts | — | — | — | — | — | F |
| Resolve disputes / complaints | Raises only | S (own store's disputes) | S | — | — | F |
| View / export audit logs | — | S (own store's actions) | S (own branch's actions) | — | S (own actions) | F |
| Rate store / product / delivery | F (own orders) | — | — | — | — | — |
| Manage own availability (online/offline) | — | — | — | — | F | — |
| View own earnings / delivery history | — | — | — | — | F | — |
| Manage payment gateway settings | — | R | — | — | — | F |

## 4. Notes on Scoped Permissions

- **Supermarket owner vs. branch manager:** an owner has **F**ull rights across every branch under their business; a branch manager has the same operational rights but **S**coped to only their assigned branch. An owner can also act as a de facto branch manager for any of their branches, but the reverse never applies.
- **Refund/cancellation approval thresholds:** branch managers and owners can approve refunds up to a configurable monetary threshold (set by platform admin per subscription tier). Anything above that threshold, or any refund tied to a formally escalated dispute, requires platform administrator approval (**A**). This prevents a store from unilaterally approving high-value refunds that might indicate fraud.
- **Store employees/pickers never touch pricing, offers, or account/staff management** — their permission surface is intentionally narrow and task-scoped (pick, pack, flag discrepancy, mark ready) since they operate on shared shop-floor devices.
- **Delivery partners never see catalogue, pricing, or inventory data** — they operate purely on assignment, navigation, OTP, and earnings data. This limits blast radius if a delivery partner account is compromised.
- **Customers can only manage their own cart/orders/addresses/reviews** — there is no concept of a customer viewing another customer's data, ever, at any layer.
- **Platform administrators are the only role with cross-tenant visibility.** Every other role is confined to their own store, branch, or self scope. This is the practical enforcement of the "strong supermarket data isolation" system requirement — see `08-security-and-fraud-control.md` §8.
- **Read-only (R) rows for higher roles** (e.g. supermarket owner has R on "Accept/reject incoming orders") reflect visibility for oversight without operational interference — an owner can see that a branch manager accepted an order, but day-to-day accept/reject action lives with the branch.

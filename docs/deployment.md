# Making the site reachable by your examiner

`http://localhost:8080` only works on the machine that is running the
application. This guide covers the two realistic ways to let somebody else
open it, and what to do first so the site is safe on the public internet.

---

## First: read this before you put the site online

The REST API has **no authentication of its own**. Anyone who can reach the
URL can call `DELETE /api/projects/1` and delete your demonstration data —
possibly in the middle of your viva. On `localhost` that does not matter,
because only you can reach it. On a public URL it matters a great deal.

The project ships with a **single shared password** for exactly this
situation. Set two environment variables wherever you host it:

```
ACCESS_USERNAME=viva
ACCESS_PASSWORD=choose-something-long-and-random
```

The browser then asks for that username and password once per visit, and
you give the pair to your examiner. Every page and every API call is
covered, including the destructive ones.

Leave both variables **unset** on your own machine and nothing changes —
the site behaves exactly as it does today. The code is
`backend/src/main/java/com/sist/rms/config/AccessGateFilter.java`, and it is
about 40 lines of plain Java with no extra library.

> This is HTTP Basic authentication. It is a gate to keep strangers out of a
> student demonstration — it is not a real user-account system, and it is
> only safe over HTTPS. Every option below gives you HTTPS automatically.

---

## Option A — a temporary public link from your own laptop

**Best when your viva is at a known time and you will be at your machine.**

Nothing is hosted. Your laptop keeps running the application, and a tunnel
gives it a public HTTPS address that anybody can open.

**Advantages:** free, no account, no card, takes two minutes, and the
database stays on your machine. You switch it off when you are done.

**Disadvantages:** your laptop must stay awake and online, and the address
changes every time you restart the tunnel.

### Steps

1. Start MySQL and the application as usual, and check
   <http://localhost:8080> works.

2. Install `cloudflared` — Cloudflare's tunnel tool. No account is needed.
   * Windows: `winget install --id Cloudflare.cloudflared`
   * macOS: `brew install cloudflared`
   * Linux: download from
     <https://github.com/cloudflare/cloudflared/releases>

3. In a second terminal:

   ```bash
   cloudflared tunnel --url http://localhost:8080
   ```

4. It prints a public address like:

   ```
   https://random-words-here.trycloudflare.com
   ```

   Send that to your examiner. It stays alive until you press Ctrl+C.

Because this address is public, start the application with the password set:

```bash
# macOS / Linux
ACCESS_USERNAME=viva ACCESS_PASSWORD='choose-something-long' mvn spring-boot:run

# Windows PowerShell
$env:ACCESS_USERNAME="viva"; $env:ACCESS_PASSWORD="choose-something-long"; mvn spring-boot:run
```

---

## Option B — host it properly, online all the time

**Best when your examiner needs to browse it whenever they like.**

You need two things: somewhere to run the application, and a MySQL database.
The repository already contains a `Dockerfile`, so the hosting service can
build and run the project without you installing anything.

Free tiers change often, so check current limits before you rely on one.
Two combinations that work well:

| | Application | Database |
|---|---|---|
| **Simplest** | Railway | Railway's own MySQL, in the same project |
| **Free-tier friendly** | Render (web service) | Aiven or Clever Cloud free MySQL |

### 1. Create the database first

Create a MySQL instance on your chosen service. It will give you a host,
port, database name, username and password. Then load the schema from your
own machine — note these are the **`-hosted`** files, which leave out the
`CREATE DATABASE` and `USE` statements that managed services do not allow:

```bash
mysql -h <host> -P <port> -u <user> -p <database> < database/schema-hosted.sql
mysql -h <host> -P <port> -u <user> -p <database> < database/sample-data-hosted.sql
```

### 2. Deploy the application

Point the service at your GitHub repository and the branch
`claude/resource-allocation-capstone-d3nu74`. It will detect the
`Dockerfile` at the repository root and build from it.

Then set these environment variables in the service's dashboard:

| Variable | Value |
|---|---|
| `DB_URL` | `jdbc:mysql://<host>:<port>/<database>?useSSL=true&serverTimezone=Asia/Kolkata` |
| `DB_USERNAME` | the database username |
| `DB_PASSWORD` | the database password |
| `ACCESS_USERNAME` | `viva` |
| `ACCESS_PASSWORD` | a long random password |

Do **not** set `PORT` — the hosting service sets it for you, and
`application.properties` already reads it.

Nothing here is written into a file that goes to GitHub, so your database
password never appears in the repository.

### 3. Check it worked

Open the address the service gives you. The browser should ask for the
username and password, then show the login page. Sign in as
`manager` / `manager123` and confirm the dashboard shows the sample data.

### A warning about free tiers

Free web services usually **go to sleep** after 15 minutes of no traffic,
and the next visit takes 30–60 seconds to wake up. If your examiner opens a
sleeping site they may think it is broken. Open the page yourself a minute
beforehand, or tell them the first load is slow.

---

## Option C — just demonstrate it on your own machine

If your examiner will be in the room, or watching a screen share, this is
the normal way to present a capstone project and by far the least risky.
Nothing is exposed to the internet, nothing can go to sleep, and no free
tier can expire the night before.

Follow the setup steps in `README.md` and open <http://localhost:8080>.

---

## Checklist before you share a public address

- [ ] `ACCESS_USERNAME` and `ACCESS_PASSWORD` are set, and opening the site
      in a private browser window really does ask for them.
- [ ] The database password is set as an environment variable, not typed
      into `application.properties` and committed.
- [ ] The demo passwords in the `users` table are still the harmless
      `manager123` style ones — never reuse a real password there, since
      they are stored as plain text.
- [ ] You have opened every page yourself on the public address.
- [ ] You know how to reload the sample data if the demo data gets changed:
      re-run the two `-hosted` SQL files.
- [ ] Take screenshots or a short screen recording as a backup, in case the
      hosting service is down on the day.

---

## Frequently hit problems

| Problem | Cause and fix |
|---|---|
| Browser keeps asking for the password | The username or password does not match. Check for stray spaces in the environment variable. |
| `Communications link failure` in the logs | `DB_URL` is wrong, or the database is not reachable from the hosting service. Some providers require `useSSL=true`. |
| `Table 'x' doesn't exist` | The two `-hosted` SQL files were not loaded into the hosted database. |
| Site loads but every panel is empty | `sample-data-hosted.sql` was not loaded. |
| First visit takes a minute | The free tier put the service to sleep. Open it yourself beforehand. |
| `Access denied for user` | Wrong `DB_USERNAME` or `DB_PASSWORD`. |

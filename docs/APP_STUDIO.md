# In-game App Studio

Studio apps are declarative text pages with an optional connection to a service. They appear on every E.D.E.N. device in the same world. No outside code editor or database is required for these apps.

## Create a community information app

1. Open your device and select **App Studio**.
2. Enter app ID `town_info`, title `Town Info`, and your information in App text.
3. Click **Publish app**.
4. Return **Home** and open **Town Info**. Use the arrows if the catalog spans multiple pages.

To edit, use the same app ID and publish again. Only its original author may edit it. Use a new ID to create another app. The world catalog holds 128 Studio apps.

## Create a shared news service

1. In Studio, set ID `town_news` and App text to the latest news.
2. Click **Publish service**. This creates `studio:town_news`.
3. Change ID to `town_info`, enter `studio:town_news` in Service link, and click **Link service**.
4. Open **Town Info**. Click **Run service** to fetch the bulletin. Studio bulletins ignore the JSON argument field.
5. To change the bulletin, return to Studio, use ID `town_news`, edit its text and click **Publish service** again.

The world holds 128 bulletin services. Only the original author may change a service. Publishing an existing app replaces its text/title and clears its service link; link the service again after publishing edits.

Titles: up to 32 characters. IDs: 1–32 lowercase letters/digits/underscores. Page/bulletin text: up to 1024 characters. No raw scripts, filesystem access or console commands are available from Studio.

## Advanced services

Trusted Java addons may register richer service behavior and screen renderers through the SDK. Their service IDs can be linked from Studio too. Each service must validate its own permissions and arguments. A JSON argument field is available in a linked app.

An advanced visual editor, multiple-page forms, installed-app permissions, transport and bank templates remain future work. The current in-game toolkit is the text-app/bulletin subset described above.

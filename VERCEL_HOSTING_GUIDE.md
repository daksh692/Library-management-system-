# Hosting on Vercel (and other services)

Vercel is an incredible platform, but it is specifically designed for frontend applications (like your React/Vite app) and Serverless functions. It **does not** support long-running Java Spring Boot backend applications.

To host your full-stack Library Management System online, you will need a **split deployment strategy**:
1. **Frontend:** Vercel (Free)
2. **Backend:** Render, Railway, or Heroku (Free tiers available)
3. **Database:** MongoDB Atlas (Free tier available)

---

## Step 1: Host the Database (MongoDB Atlas)
Your hosted backend needs an online database, as it can no longer use `localhost`.
1. Go to [MongoDB Atlas](https://www.mongodb.com/cloud/atlas/register) and create a free account.
2. Create an **M0 (Free) Cluster**.
3. Under **Database Access**, create a user (e.g., `admin` / `password123`).
4. Under **Network Access**, add `0.0.0.0/0` so your backend can connect from anywhere.
5. Click **Connect -> Drivers -> Java** and copy the connection string.
   - It will look like: `mongodb+srv://admin:password123@cluster0.../lms?retryWrites=true&w=majority`

---

## Step 2: Host the Backend (Render or Railway)
Since Vercel cannot host Spring Boot, we will use Render (which is free and easy).

### Option A: Render.com
1. Go to [Render](https://render.com/) and link your GitHub account.
2. Click **New +** -> **Web Service**.
3. Select your `Library-management-system-` repository.
4. **Configuration:**
4. **Configuration (CRITICAL STEP):**
   - **Root Directory:** You **MUST** type `backend` here. If you leave this blank, Render will see your React `package.json` in the root and try to build it as a Node.js app (which gives the `mvn: command not found` error!).
   - **Environment:** Select `Java` from the dropdown (if it says Node, you will get an error).
   - **Build Command:** `mvn clean package -DskipTests`
   - **Start Command:** `java -jar target/lms-0.0.1-SNAPSHOT.jar`
5. **Environment Variables:**
   - Add `MONGODB_URI` and paste your Atlas connection string.
   - Add `JWT_SECRET` and generate a long random string (e.g., `my_super_secret_key_that_is_at_least_32_bytes`).
6. Click **Create Web Service** (or **Save Changes** and click **Manual Deploy** -> **Deploy latest commit**). Wait for it to build. Once done, copy the Render URL (e.g., `https://lms-backend.onrender.com`).

---

## Step 3: Host the Frontend (Vercel)
Now we host the React application on Vercel, pointing it to your newly deployed backend.

### First: Update your Frontend API URL
Before deploying to Vercel, Vercel needs to know where your backend lives.
In your project, the API URL is likely hardcoded to `http://localhost:8080/api` in `frontend/src/services/api.js`.

You should modify `frontend/src/services/api.js` so it uses an environment variable:
```javascript
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
});
```
*(Push this change to GitHub if you haven't already).*

### Second: Deploy to Vercel
1. Go to [Vercel](https://vercel.com/) and link your GitHub account.
2. Click **Add New -> Project** and import your `Library-management-system-` repository.
3. **Configuration:**
   - **Framework Preset:** `Vite`
   - **Root Directory:** `frontend` (very important!).
4. **Environment Variables:**
   - Add `VITE_API_URL` and set the value to your backend URL (e.g., `https://lms-backend.onrender.com/api`).
5. Click **Deploy**.

Vercel will build your React application and give you a live URL (e.g., `https://library-system.vercel.app`).

---

## You're Done!
You now have a fully deployed, production-ready application!
- The user goes to your **Vercel** URL.
- The Vercel app makes requests to your **Render** backend.
- The Render backend saves and reads data from your **MongoDB Atlas** database.

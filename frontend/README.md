# MealCraft Frontend

React frontend application for MealCraft - Plan smarter. Eat better.

## Technology Stack

- **Framework**: React 18+
- **Build Tool**: Vite
- **Styling**: Tailwind CSS 3.x
- **Routing**: React Router 6.x
- **HTTP Client**: Axios
- **Icons**: Lucide React
- **Date Handling**: date-fns
- **Drag & Drop**: @dnd-kit/core

## Project Structure

```
frontend/
├── src/
│   ├── components/        # Reusable components
│   │   ├── auth/         # Authentication components
│   │   └── layout/        # Layout components
│   ├── contexts/         # React Context providers
│   ├── pages/            # Page components
│   │   ├── auth/         # Authentication pages
│   │   └── ...           # Main pages
│   ├── utils/            # Utility functions
│   ├── App.jsx           # Main app component
│   ├── main.jsx          # Entry point
│   └── index.css         # Global styles
├── index.html
├── package.json
├── vite.config.js
└── tailwind.config.js
```

## Setup Instructions

### Prerequisites

1. **Node.js 18+** installed
2. **npm** or **yarn** package manager

### Installation

1. **Install dependencies**:
   ```bash
   cd frontend
   npm install
   ```

2. **Configure API endpoint** (optional):
   - Create `.env` file:
     ```
     VITE_API_BASE_URL=http://localhost:8080
     ```

### Running the Application

1. **Start development server**:
   ```bash
   npm run dev
   ```

2. **Access the application**:
   - URL: `http://localhost:5173`

3. **Build for production**:
   ```bash
   npm run build
   ```

4. **Preview production build**:
   ```bash
   npm run preview
   ```

## Features

### Authentication
- User registration and login
- JWT token-based authentication
- Protected routes
- Session persistence

### Dashboard
- Expiring items alerts
- Expired items alerts
- Low-stock alerts
- Quick statistics
- Recent saved recipes
- Quick action buttons

### Pantry Management
- Add, edit, delete pantry items
- Category organization (4 categories)
- Expiration tracking with visual indicators
- Low-stock alerts
- Filter by status (expiring, expired, low-stock)

### Recipe Management
- AI-powered recipe suggestions (TheMealDB API - completely free, no API key needed)
- Save suggested recipes
- Search saved recipes
- View recipe details
- Edit saved recipes
- Expiry priority indicators
- Smart ingredient matching with parallel API calls

### Meal Planning
- Weekly calendar view
- Drag-and-drop recipe assignment
- Quick-add recipes
- View meal plan details

### Shopping List
- Auto-generate from meal plans
- Manual item addition
- Mark items as purchased
- Clear purchased items

### Profile Management
- View and edit profile
- Profile icon with initials
- Color-coded profile icons

## Development

### Adding New Pages

1. Create page component in `src/pages/`
2. Add route in `src/App.jsx`
3. Add navigation link in `src/components/layout/Sidebar.jsx`

### Styling

- Uses Tailwind CSS utility classes
- Custom colors defined in `tailwind.config.js`
- Responsive design with mobile-first approach

### State Management

- **AuthContext**: User authentication state
- **NotificationContext**: Global notifications
- Component-level state for page-specific data

### API Integration

- Axios instance configured in `src/utils/axios.js`
- Automatic JWT token injection
- Error handling and 401 redirect

## Production Considerations

1. Set proper API base URL in environment variables
2. Enable HTTPS
3. Optimize bundle size
4. Add error boundaries
5. Implement loading states
6. Add comprehensive error handling
7. Optimize images and assets
8. Add analytics (optional)
9. Implement PWA features (optional)
10. Add comprehensive testing




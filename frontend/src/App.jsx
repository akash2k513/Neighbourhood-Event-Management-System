import React, { useState } from 'react';
import { AuthProvider } from './context/AuthContext';
import EventList from './components/EventList';
import EventCalendar from './components/EventCalendar';
import './styles/app.css';

function App() {
    const [page, setPage] = useState("events");

    return (
        <AuthProvider>
            <div className="app">

                <header className="navbar">
                    <div className="navbar-inner">
                        <a href="#" className="navbar-brand">
                            🏘️ NeighborHub
                        </a>
                        <nav className="navbar-links">
                            <button
                                className={`nav-link ${page === "events" ? "active" : ""}`}
                                onClick={() => setPage("events")}
                            >Events</button>
                            <button
                                className={`nav-link ${page === "calendar" ? "active" : ""}`}
                                onClick={() => setPage("calendar")}
                            >Calendar</button>
                            <a href="#" className="nav-link">Zones</a>
                            <a href="#" className="nav-link">Resources</a>
                        </nav>
                        <div className="navbar-actions">
                            <button className="btn-outline">Log in</button>
                            <button className="btn-primary">Sign up</button>
                        </div>
                    </div>
                </header>

                <main className="page-content">
                    {page === "events"   && <EventList />}
                    {page === "calendar" && <EventCalendar />}
                </main>

                <footer className="footer">
                    © 2026 NeighborHub · Neighborhood Event Management System
                </footer>

            </div>
        </AuthProvider>
    );
}

export default App;

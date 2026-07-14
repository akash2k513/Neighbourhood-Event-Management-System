import React from 'react';
import EventList from './components/EventList';
import './styles/app.css';

function App() {
    return (
        <div className="app">

            <header className="navbar">
                <div className="navbar-inner">
                    <a href="#" className="navbar-brand">
                        🏘️ NeighborHub
                    </a>
                    <nav className="navbar-links">
                        <a href="#" className="nav-link active">Events</a>
                        <a href="#" className="nav-link">Calendar</a>
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
                <EventList />
            </main>

            <footer className="footer">
                © 2026 NeighborHub · Neighborhood Event Management System
            </footer>

        </div>
    );
}

export default App;

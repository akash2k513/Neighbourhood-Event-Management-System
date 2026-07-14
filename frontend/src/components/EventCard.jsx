import React from 'react';
import './EventCard.css';

const STATUS_COLORS = {
    DRAFT:            { color: '#92400e', bg: '#fef3c7' },
    PENDING_APPROVAL: { color: '#5b21b6', bg: '#ede9fe' },
    APPROVED:         { color: '#065f46', bg: '#d1fae5' },
    REJECTED:         { color: '#991b1b', bg: '#fee2e2' },
    PUBLISHED:        { color: '#065f46', bg: '#d1fae5' },
    CANCELLED:        { color: '#991b1b', bg: '#fee2e2' },
    COMPLETED:        { color: '#1e40af', bg: '#dbeafe' },
};

function EventCard({ event, view }) {
    const fmt = (d) =>
        new Date(d).toLocaleString('en-IN', {
            day: '2-digit', month: 'short', year: 'numeric',
            hour: '2-digit', minute: '2-digit',
        });

    const statusStyle = STATUS_COLORS[event.status] ?? { color: '#374151', bg: '#f3f4f6' };

    return (
        <article className={`ec-card ${view}`}>

            <div className="ec-header">
                <span className="ec-status" style={{ color: statusStyle.color, background: statusStyle.bg }}>
                    {event.status?.replace('_', ' ')}
                </span>
                <span className="ec-category">{event.category}</span>
            </div>

            <h3 className="ec-title">{event.title}</h3>

            {event.description && (
                <p className="ec-desc">{event.description}</p>
            )}

            <ul className="ec-details">
                <li>📍 {event.location}</li>
                <li>📅 {fmt(event.startTime)}</li>
                <li>⏰ {fmt(event.endTime)}</li>
                {event.zoneName     && <li>🗺️ {event.zoneName}</li>}
                {event.organizerName && <li>👤 {event.organizerName}</li>}
                <li>👥 {event.registeredCount} / {event.capacity} registered</li>
            </ul>

            <button className="ec-btn">View Details</button>

        </article>
    );
}

export default EventCard;

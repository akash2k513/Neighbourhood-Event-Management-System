import React from 'react';
import './EventList.css';

function SkeletonCard() {
    return (
        <div className="skeleton-card">
            <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
                <div className="skeleton-line" style={{ width: 72, height: 20, marginBottom: 0 }} />
                <div className="skeleton-line" style={{ width: 60, height: 20, marginBottom: 0 }} />
            </div>
            <div className="skeleton-line" style={{ width: '70%', height: 20 }} />
            <div className="skeleton-line" style={{ width: '90%', height: 13 }} />
            <div className="skeleton-line" style={{ width: '55%', height: 13 }} />
            <div className="skeleton-line" style={{ width: '50%', height: 13 }} />
            <div className="skeleton-line" style={{ width: '60%', height: 13 }} />
            <div className="skeleton-line" style={{ width: 90, height: 32, marginTop: 6, borderRadius: 6 }} />
        </div>
    );
}

export default SkeletonCard;

import React, { useEffect, useState } from "react";
import registrationService from "../services/registrationService";
import { useAuth } from "../context/AuthContext";
import "./EventDetailModal.css";

const fmt = (d) =>
    new Date(d).toLocaleString("en-IN", {
        day: "2-digit", month: "short", year: "numeric",
        hour: "2-digit", minute: "2-digit",
    });

export default function EventDetailModal({ event, onClose, myRegistrations = [] }) {
    const { isLoggedIn } = useAuth();

    const myReg = myRegistrations.find((r) => r.event?.id === event.id);
    const isRegistered  = myReg?.status === "REGISTERED";
    const isWaitlisted  = myReg?.status === "WAITLISTED";

    const [regStatus, setRegStatus]   = useState(
        isRegistered ? "registered" : isWaitlisted ? "waitlisted" : "none"
    );
    const [regMsg, setRegMsg]         = useState("");
    const [regLoading, setRegLoading] = useState(false);

    const [feedback, setFeedback]     = useState([]);
    const [rating, setRating]         = useState(5);
    const [comment, setComment]       = useState("");
    const [fbMsg, setFbMsg]           = useState("");
    const [fbLoading, setFbLoading]   = useState(false);

    useEffect(() => {
        registrationService.getFeedback(event.id)
            .then((r) => setFeedback(r.data))
            .catch(() => {});
    }, [event.id]);

    async function handleRegister() {
        setRegLoading(true); setRegMsg("");
        try {
            const res = await registrationService.register(event.id);
            const msg = res.data;
            setRegStatus(msg.includes("waitlist") ? "waitlisted" : "registered");
            setRegMsg(msg);
        } catch (e) {
            setRegMsg(e.response?.data?.message || "Registration failed.");
        } finally { setRegLoading(false); }
    }

    async function handleUnregister() {
        setRegLoading(true); setRegMsg("");
        try {
            await registrationService.unregister(event.id);
            setRegStatus("none");
            setRegMsg("Unregistered successfully.");
        } catch (e) {
            setRegMsg(e.response?.data?.message || "Unregistration failed.");
        } finally { setRegLoading(false); }
    }

    async function handleFeedback(e) {
        e.preventDefault();
        setFbLoading(true); setFbMsg("");
        try {
            await registrationService.submitFeedback(event.id, rating, comment);
            setFbMsg("Feedback submitted!");
            const res = await registrationService.getFeedback(event.id);
            setFeedback(res.data);
            setComment("");
        } catch (err) {
            setFbMsg(err.response?.data?.message || "Failed to submit feedback.");
        } finally { setFbLoading(false); }
    }

    const isFull = event.registeredCount >= event.capacity;
    const canRegister = event.status === "PUBLISHED" || event.status === "APPROVED";

    return (
        <div className="edm-overlay" role="dialog" aria-modal="true" onClick={(e) => e.target === e.currentTarget && onClose()}>
            <div className="edm-panel">
                <button className="edm-close" onClick={onClose} aria-label="Close">✕</button>

                <div className="edm-header">
                    <span className="edm-category">{event.category}</span>
                    <span className="edm-status">{event.status?.replace("_", " ")}</span>
                </div>

                <h2 className="edm-title">{event.title}</h2>
                {event.description && <p className="edm-desc">{event.description}</p>}

                <ul className="edm-meta">
                    <li>📍 {event.location}</li>
                    <li>📅 {fmt(event.startTime)} — {fmt(event.endTime)}</li>
                    {event.zoneName     && <li>🗺️ {event.zoneName}</li>}
                    {event.venueName    && <li>🏛️ {event.venueName}</li>}
                    {event.organizerName && <li>👤 {event.organizerName}</li>}
                    <li>👥 {event.registeredCount} / {event.capacity} registered{isFull && " · Full"}</li>
                </ul>

                {/* ── Registration ── */}
                {isLoggedIn && canRegister && (
                    <div className="edm-reg">
                        {regStatus === "none" && (
                            <button className="edm-btn primary" onClick={handleRegister} disabled={regLoading}>
                                {regLoading ? "Registering…" : isFull ? "Join Waitlist" : "Register"}
                            </button>
                        )}
                        {regStatus === "registered" && (
                            <button className="edm-btn danger" onClick={handleUnregister} disabled={regLoading}>
                                {regLoading ? "Cancelling…" : "Cancel Registration"}
                            </button>
                        )}
                        {regStatus === "waitlisted" && (
                            <button className="edm-btn danger" onClick={handleUnregister} disabled={regLoading}>
                                {regLoading ? "Removing…" : "Leave Waitlist"}
                            </button>
                        )}
                        {regMsg && <p className="edm-msg">{regMsg}</p>}
                    </div>
                )}
                {!isLoggedIn && canRegister && (
                    <p className="edm-login-hint">Log in to register for this event.</p>
                )}

                {/* ── Feedback ── */}
                <div className="edm-feedback">
                    <h3 className="edm-section-title">Feedback ({feedback.length})</h3>

                    {feedback.length > 0 && (
                        <ul className="edm-fb-list">
                            {feedback.map((fb) => (
                                <li key={fb.id} className="edm-fb-item">
                                    <span className="edm-fb-stars">{"★".repeat(fb.rating)}{"☆".repeat(5 - fb.rating)}</span>
                                    {fb.comment && <span className="edm-fb-comment">{fb.comment}</span>}
                                </li>
                            ))}
                        </ul>
                    )}

                    {isLoggedIn && (
                        <form className="edm-fb-form" onSubmit={handleFeedback}>
                            <label className="edm-label">
                                Rating
                                <select value={rating} onChange={(e) => setRating(Number(e.target.value))}>
                                    {[1,2,3,4,5].map((n) => <option key={n} value={n}>{n} ★</option>)}
                                </select>
                            </label>
                            <textarea
                                className="edm-textarea"
                                placeholder="Leave a comment (optional)"
                                value={comment}
                                onChange={(e) => setComment(e.target.value)}
                                rows={3}
                            />
                            <button className="edm-btn primary" type="submit" disabled={fbLoading}>
                                {fbLoading ? "Submitting…" : "Submit Feedback"}
                            </button>
                            {fbMsg && <p className="edm-msg">{fbMsg}</p>}
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
}

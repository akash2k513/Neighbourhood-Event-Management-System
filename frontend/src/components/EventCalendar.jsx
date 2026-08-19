import React, { useEffect, useState, useCallback } from "react";
import eventService from "../services/eventService";
import registrationService from "../services/registrationService";
import { useAuth } from "../context/AuthContext";
import EventDetailModal from "./EventDetailModal";
import "./EventCalendar.css";

const VIEWS = ["month", "week", "day"];

const DAYS   = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MONTHS = ["January","February","March","April","May","June",
                "July","August","September","October","November","December"];

function isSameDay(a, b) {
    return a.getFullYear() === b.getFullYear() &&
           a.getMonth()    === b.getMonth()    &&
           a.getDate()     === b.getDate();
}

function eventFallsOnDay(event, day) {
    const start = new Date(event.startTime);
    const end   = new Date(event.endTime);
    return start <= day && end >= new Date(day.getFullYear(), day.getMonth(), day.getDate(), 23, 59, 59);
}

function startOfWeek(date) {
    const d = new Date(date);
    d.setDate(d.getDate() - d.getDay());
    d.setHours(0,0,0,0);
    return d;
}

export default function EventCalendar() {
    const { isLoggedIn } = useAuth();
    const today = new Date();

    const [view, setView]               = useState("month");
    const [cursor, setCursor]           = useState(new Date(today.getFullYear(), today.getMonth(), 1));
    const [events, setEvents]           = useState([]);
    const [myRegIds, setMyRegIds]       = useState(new Set());
    const [myRegistrations, setMyRegs]  = useState([]);
    const [selected, setSelected]       = useState(null);
    const [loading, setLoading]         = useState(false);

    // Compute window start/end for the current view
    const windowBounds = useCallback(() => {
        if (view === "month") {
            const start = new Date(cursor.getFullYear(), cursor.getMonth(), 1);
            const end   = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 0, 23, 59, 59);
            return { start, end };
        }
        if (view === "week") {
            const start = startOfWeek(cursor);
            const end   = new Date(start); end.setDate(start.getDate() + 6); end.setHours(23,59,59);
            return { start, end };
        }
        // day
        const start = new Date(cursor); start.setHours(0,0,0,0);
        const end   = new Date(cursor); end.setHours(23,59,59);
        return { start, end };
    }, [view, cursor]);

    useEffect(() => {
        const { start, end } = windowBounds();
        setLoading(true);
        eventService.getEvents({
            page: 0, size: 100, sortBy: "startTime", sortDirection: "asc",
            startTime: start.toISOString(), endTime: end.toISOString(),
        })
        .then((r) => setEvents(r.data.content ?? []))
        .catch(() => setEvents([]))
        .finally(() => setLoading(false));
    }, [windowBounds]);

    useEffect(() => {
        if (!isLoggedIn) return;
        registrationService.getMyRegistrations()
            .then((r) => {
                setMyRegs(r.data);
                setMyRegIds(new Set(r.data.map((reg) => reg.event?.id)));
            })
            .catch(() => {});
    }, [isLoggedIn]);

    // ── Navigation ────────────────────────────────────────────────────
    function navigate(dir) {
        setCursor((prev) => {
            const d = new Date(prev);
            if (view === "month") d.setMonth(d.getMonth() + dir);
            else if (view === "week") d.setDate(d.getDate() + dir * 7);
            else d.setDate(d.getDate() + dir);
            return d;
        });
    }

    function goToday() {
        setCursor(view === "month"
            ? new Date(today.getFullYear(), today.getMonth(), 1)
            : new Date(today));
    }

    // ── Label ─────────────────────────────────────────────────────────
    function label() {
        if (view === "month") return `${MONTHS[cursor.getMonth()]} ${cursor.getFullYear()}`;
        if (view === "week") {
            const s = startOfWeek(cursor);
            const e = new Date(s); e.setDate(s.getDate() + 6);
            return `${s.getDate()} ${MONTHS[s.getMonth()]} – ${e.getDate()} ${MONTHS[e.getMonth()]} ${e.getFullYear()}`;
        }
        return `${cursor.getDate()} ${MONTHS[cursor.getMonth()]} ${cursor.getFullYear()}`;
    }

    // ── Month grid ────────────────────────────────────────────────────
    function buildMonthDays() {
        const year  = cursor.getFullYear();
        const month = cursor.getMonth();
        const first = new Date(year, month, 1).getDay();
        const total = new Date(year, month + 1, 0).getDate();
        const days  = [];
        for (let i = 0; i < first; i++) days.push(null);
        for (let d = 1; d <= total; d++) days.push(new Date(year, month, d));
        return days;
    }

    // ── Week days ─────────────────────────────────────────────────────
    function buildWeekDays() {
        const s = startOfWeek(cursor);
        return Array.from({ length: 7 }, (_, i) => {
            const d = new Date(s); d.setDate(s.getDate() + i); return d;
        });
    }

    function eventsForDay(day) {
        return events.filter((e) => eventFallsOnDay(e, day));
    }

    function EventPill({ event }) {
        const mine = myRegIds.has(event.id);
        return (
            <button
                className={`cal-pill ${mine ? "mine" : ""}`}
                onClick={() => setSelected(event)}
                title={event.title}
            >
                {mine && <span className="cal-dot" />}
                {event.title}
            </button>
        );
    }

    // ── Render ────────────────────────────────────────────────────────
    return (
        <div className="cal-root">
            {/* Toolbar */}
            <div className="cal-toolbar">
                <div className="cal-nav">
                    <button className="cal-nav-btn" onClick={() => navigate(-1)} aria-label="Previous">‹</button>
                    <span className="cal-label">{label()}</span>
                    <button className="cal-nav-btn" onClick={() => navigate(1)} aria-label="Next">›</button>
                    <button className="cal-today-btn" onClick={goToday}>Today</button>
                </div>
                <div className="cal-view-toggle">
                    {VIEWS.map((v) => (
                        <button
                            key={v}
                            className={`cal-view-btn ${view === v ? "active" : ""}`}
                            onClick={() => { setView(v); setCursor(new Date(today)); }}
                        >
                            {v.charAt(0).toUpperCase() + v.slice(1)}
                        </button>
                    ))}
                </div>
            </div>

            {loading && <div className="cal-loading">Loading events…</div>}

            {/* ── Month view ── */}
            {view === "month" && (
                <div className="cal-month">
                    <div className="cal-weekdays">
                        {DAYS.map((d) => <div key={d} className="cal-weekday">{d}</div>)}
                    </div>
                    <div className="cal-grid">
                        {buildMonthDays().map((day, i) => {
                            if (!day) return <div key={`empty-${i}`} className="cal-cell empty" />;
                            const dayEvents = eventsForDay(day);
                            const isToday   = isSameDay(day, today);
                            return (
                                <div key={day.toISOString()} className={`cal-cell ${isToday ? "today" : ""}`}>
                                    <span className="cal-day-num">{day.getDate()}</span>
                                    <div className="cal-pills">
                                        {dayEvents.slice(0, 3).map((e) => <EventPill key={e.id} event={e} />)}
                                        {dayEvents.length > 3 && (
                                            <span className="cal-more">+{dayEvents.length - 3} more</span>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}

            {/* ── Week view ── */}
            {view === "week" && (
                <div className="cal-week">
                    {buildWeekDays().map((day) => {
                        const dayEvents = eventsForDay(day);
                        const isToday   = isSameDay(day, today);
                        return (
                            <div key={day.toISOString()} className={`cal-week-col ${isToday ? "today" : ""}`}>
                                <div className="cal-week-header">
                                    <span className="cal-week-day">{DAYS[day.getDay()]}</span>
                                    <span className={`cal-week-num ${isToday ? "today" : ""}`}>{day.getDate()}</span>
                                </div>
                                <div className="cal-week-events">
                                    {dayEvents.map((e) => <EventPill key={e.id} event={e} />)}
                                    {dayEvents.length === 0 && <span className="cal-no-events">—</span>}
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}

            {/* ── Day view ── */}
            {view === "day" && (
                <div className="cal-day">
                    <div className="cal-day-header">
                        <span className={`cal-day-title ${isSameDay(cursor, today) ? "today" : ""}`}>
                            {DAYS[cursor.getDay()]}, {cursor.getDate()} {MONTHS[cursor.getMonth()]} {cursor.getFullYear()}
                        </span>
                    </div>
                    <div className="cal-day-events">
                        {eventsForDay(cursor).length === 0 && (
                            <p className="cal-no-events">No events on this day.</p>
                        )}
                        {eventsForDay(cursor).map((e) => {
                            const mine = myRegIds.has(e.id);
                            return (
                                <button
                                    key={e.id}
                                    className={`cal-day-event ${mine ? "mine" : ""}`}
                                    onClick={() => setSelected(e)}
                                >
                                    <div className="cal-day-event-title">
                                        {mine && <span className="cal-dot" />}
                                        {e.title}
                                    </div>
                                    <div className="cal-day-event-time">
                                        {new Date(e.startTime).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}
                                        {" – "}
                                        {new Date(e.endTime).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}
                                    </div>
                                    <div className="cal-day-event-meta">
                                        📍 {e.location} · 👥 {e.registeredCount}/{e.capacity}
                                    </div>
                                </button>
                            );
                        })}
                    </div>
                </div>
            )}

            {selected && (
                <EventDetailModal
                    event={selected}
                    myRegistrations={myRegistrations}
                    onClose={() => setSelected(null)}
                />
            )}
        </div>
    );
}

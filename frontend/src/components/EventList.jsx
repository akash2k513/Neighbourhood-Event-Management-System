import React, { useEffect, useState } from "react";
import eventService from "../services/eventService";
import EventCard from "./EventCard";
import SkeletonCard from "./SkeletonCard";
import "./EventList.css";

const CATEGORIES = ["SOCIAL","SPORTS","CULTURAL","EDUCATIONAL","HEALTH","ENVIRONMENT","OTHER"];
const STATUSES   = ["DRAFT","PENDING_APPROVAL","APPROVED","REJECTED","PUBLISHED","CANCELLED","COMPLETED"];

function EventList() {

    const [events, setEvents]         = useState([]);
    const [loading, setLoading]       = useState(true);
    const [error, setError]           = useState("");
    const [totalElements, setTotal]   = useState(0);

    const [view, setView]             = useState("grid");
    const [keyword, setKeyword]       = useState("");
    const [category, setCategory]     = useState("");
    const [status, setStatus]         = useState("");
    const [zoneId, setZoneId]         = useState("");
    const [sortBy, setSortBy]         = useState("startTime");
    const [sortDirection, setSortDir] = useState("asc");
    const [page, setPage]             = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const size = 6;

    // Auto-switch to list on mobile
    useEffect(() => {
        const onResize = () => setView(window.innerWidth <= 768 ? "list" : "grid");
        onResize();
        window.addEventListener("resize", onResize);
        return () => window.removeEventListener("resize", onResize);
    }, []);

    const loadEvents = async () => {
        try {
            setLoading(true);
            setError("");
            const res = keyword.trim()
                ? await eventService.searchEvents({ keyword, page, size, sortBy, sortDirection })
                : await eventService.getEvents({ page, size, sortBy, sortDirection, category, status, zoneId });
            setEvents(res.data.content);
            setTotalPages(res.data.totalPages);
            setTotal(res.data.totalElements ?? res.data.content.length);
        } catch {
            setError("Failed to load events. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { loadEvents(); }, [page, keyword, category, status, zoneId, sortBy, sortDirection]);

    const resetFilters = () => {
        setKeyword(""); setCategory(""); setStatus("");
        setZoneId(""); setSortBy("startTime"); setSortDir("asc"); setPage(0);
    };

    const hasFilters = keyword || category || status || zoneId;

    return (
        <div className="el-root">

            {/* ── Section header ── */}
            <div className="el-header">
                <div>
                    <h2 className="el-title">All Events</h2>
                    {!loading && !error && (
                        <p className="el-count">
                            {totalElements} event{totalElements !== 1 ? "s" : ""} found
                        </p>
                    )}
                </div>
                <div className="el-view-toggle">
                    <button
                        className={`view-btn ${view === "grid" ? "active" : ""}`}
                        aria-label="Grid view" aria-pressed={view === "grid"}
                        onClick={() => setView("grid")}
                        title="Grid view"
                    >
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                            <rect x="0" y="0" width="7" height="7" rx="1"/>
                            <rect x="9" y="0" width="7" height="7" rx="1"/>
                            <rect x="0" y="9" width="7" height="7" rx="1"/>
                            <rect x="9" y="9" width="7" height="7" rx="1"/>
                        </svg>
                        Grid
                    </button>
                    <button
                        className={`view-btn ${view === "list" ? "active" : ""}`}
                        aria-label="List view" aria-pressed={view === "list"}
                        onClick={() => setView("list")}
                        title="List view"
                    >
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                            <rect x="0" y="1" width="16" height="2" rx="1"/>
                            <rect x="0" y="7" width="16" height="2" rx="1"/>
                            <rect x="0" y="13" width="16" height="2" rx="1"/>
                        </svg>
                        List
                    </button>
                </div>
            </div>

            {/* ── Filter panel ── */}
            <div className="filter-panel" role="toolbar" aria-label="Event filters">

                <div className="filter-row">
                    <div className="search-wrap">
                        <svg className="search-icon" width="16" height="16" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="9" cy="9" r="6"/><path d="m15 15 4 4"/>
                        </svg>
                        <input
                            type="text"
                            className="filter-search"
                            placeholder="Search events by title, location..."
                            aria-label="Search events"
                            value={keyword}
                            onChange={(e) => { setKeyword(e.target.value); setPage(0); }}
                        />
                        {keyword && (
                            <button className="search-clear" onClick={() => { setKeyword(""); setPage(0); }} aria-label="Clear search">✕</button>
                        )}
                    </div>

                    <div className="filter-selects">
                        <select aria-label="Filter by category" value={category}
                            onChange={(e) => { setCategory(e.target.value); setPage(0); }}>
                            <option value="">All Categories</option>
                            {CATEGORIES.map(c => (
                                <option key={c} value={c}>{c.charAt(0) + c.slice(1).toLowerCase()}</option>
                            ))}
                        </select>

                        <select aria-label="Filter by status" value={status}
                            onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
                            <option value="">All Status</option>
                            {STATUSES.map(s => (
                                <option key={s} value={s}>{s.replace("_", " ")}</option>
                            ))}
                        </select>

                        <input
                            type="number" min="1"
                            className="zone-input"
                            placeholder="Zone ID"
                            aria-label="Filter by zone ID"
                            value={zoneId}
                            onChange={(e) => { setZoneId(e.target.value); setPage(0); }}
                        />

                        <select aria-label="Sort by" value={sortBy}
                            onChange={(e) => { setSortBy(e.target.value); setPage(0); }}>
                            <option value="startTime">Sort: Date</option>
                            <option value="title">Sort: Title</option>
                            <option value="capacity">Sort: Capacity</option>
                        </select>

                        <button
                            className={`dir-btn ${sortDirection === "asc" ? "" : "desc"}`}
                            aria-label="Sort direction"
                            onClick={() => { setSortDir(d => d === "asc" ? "desc" : "asc"); setPage(0); }}
                            title={sortDirection === "asc" ? "Ascending" : "Descending"}
                        >
                            {sortDirection === "asc" ? "↑ Asc" : "↓ Desc"}
                        </button>

                        {hasFilters && (
                            <button className="reset-btn" onClick={resetFilters} aria-label="Reset filters">
                                Reset
                            </button>
                        )}
                    </div>
                </div>

            </div>

            {/* ── Loading skeletons ── */}
            {loading && (
                <div className={`el-grid ${view}`} role="status" aria-label="Loading events">
                    {Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} view={view} />)}
                </div>
            )}

            {/* ── Error ── */}
            {!loading && error && (
                <div className="el-error" role="alert">
                    <span className="el-error-icon">⚠️</span>
                    <p>{error}</p>
                    <button className="retry-btn" onClick={loadEvents}>Try again</button>
                </div>
            )}

            {/* ── Empty state ── */}
            {!loading && !error && events.length === 0 && (
                <div className="el-empty">
                    <span className="el-empty-icon">🔍</span>
                    <h3>No events found</h3>
                    <p>Try adjusting your filters or search term.</p>
                    {hasFilters && (
                        <button className="retry-btn" onClick={resetFilters}>Clear filters</button>
                    )}
                </div>
            )}

            {/* ── Event grid/list ── */}
            {!loading && !error && events.length > 0 && (
                <div className={`el-grid ${view}`}>
                    {events.map(event => (
                        <EventCard key={event.id} event={event} view={view} />
                    ))}
                </div>
            )}

            {/* ── Pagination ── */}
            {!loading && !error && totalPages > 1 && (
                <div className="el-pagination" role="navigation" aria-label="Pagination">
                    <button
                        className="page-btn"
                        disabled={page === 0}
                        aria-label="Previous page"
                        onClick={() => setPage(p => p - 1)}
                    >
                        ← Prev
                    </button>

                    <div className="page-numbers">
                        {Array.from({ length: totalPages }, (_, i) => (
                            <button
                                key={i}
                                className={`page-num ${i === page ? "active" : ""}`}
                                aria-label={`Page ${i + 1}`}
                                aria-current={i === page ? "page" : undefined}
                                onClick={() => setPage(i)}
                            >
                                {i + 1}
                            </button>
                        ))}
                    </div>

                    <button
                        className="page-btn"
                        disabled={page + 1 >= totalPages}
                        aria-label="Next page"
                        onClick={() => setPage(p => p + 1)}
                    >
                        Next →
                    </button>
                </div>
            )}

        </div>
    );
}

export default EventList;

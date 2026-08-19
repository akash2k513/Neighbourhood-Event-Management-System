import axios from "axios";

const BASE = "http://localhost:8080/api";

function authHeader() {
    const token = localStorage.getItem("token");
    return token ? { Authorization: `Bearer ${token}` } : {};
}

const registrationService = {
    register(eventId) {
        return axios.post(`${BASE}/events/${eventId}/register`, {}, { headers: authHeader() });
    },
    unregister(eventId) {
        return axios.delete(`${BASE}/events/${eventId}/unregister`, { headers: authHeader() });
    },
    getMyRegistrations() {
        return axios.get(`${BASE}/registrations/my`, { headers: authHeader() });
    },
    submitFeedback(eventId, rating, comment) {
        return axios.post(`${BASE}/events/${eventId}/feedback`, { rating, comment }, { headers: authHeader() });
    },
    getFeedback(eventId) {
        return axios.get(`${BASE}/events/${eventId}/feedback`);
    },
    getCalendarEvents(zoneId, startTime, endTime) {
        return axios.get(`${BASE}/events/calendar`, {
            params: { zoneId, startTime, endTime },
            headers: authHeader(),
        });
    },
};

export default registrationService;

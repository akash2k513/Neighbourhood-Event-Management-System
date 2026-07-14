import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api/events";

// Strip keys with empty-string or undefined values so backend doesn't
// receive spurious filter params like category=&status=
function clean(params) {
    return Object.fromEntries(
        Object.entries(params).filter(([, v]) => v !== "" && v !== undefined && v !== null)
    );
}

const eventService = {

    getEvents(params) {
        return axios.get(API_BASE_URL, {
            params: clean({
                page: params.page,
                size: params.size,
                sortBy: params.sortBy,
                sortDirection: params.sortDirection,
                category: params.category,
                status: params.status,
                zoneId: params.zoneId
            })
        });
    },

    searchEvents(params) {
        return axios.get(`${API_BASE_URL}/search`, {
            params: clean({
                keyword: params.keyword,
                page: params.page,
                size: params.size,
                sortBy: params.sortBy,
                sortDirection: params.sortDirection
            })
        });
    },

    getEventById(id) {
        return axios.get(`${API_BASE_URL}/${id}`);
    },

    createEvent(event) {
        return axios.post(API_BASE_URL, event);
    },

    updateEvent(id, event) {
        return axios.put(`${API_BASE_URL}/${id}`, event);
    },

    deleteEvent(id) {
        return axios.delete(`${API_BASE_URL}/${id}`);
    }
};

export default eventService;

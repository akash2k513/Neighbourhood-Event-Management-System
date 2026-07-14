import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import EventList from "../components/EventList";
import eventService from "../services/eventService";

jest.mock("../services/eventService");

const mockPage = (overrides = {}) => ({
    data: {
        content: [
            {
                id: 1,
                title: "Community Meeting",
                category: "SOCIAL",
                status: "APPROVED",
                location: "Hall A",
                startTime: "2026-08-10T10:00:00",
                endTime: "2026-08-10T12:00:00",
                capacity: 100,
                registeredCount: 20,
                description: "Monthly community meeting"
            }
        ],
        totalPages: 3,
        ...overrides
    }
});

describe("EventList", () => {

    beforeEach(() => {
        eventService.getEvents.mockResolvedValue(mockPage());
        eventService.searchEvents.mockResolvedValue(mockPage());
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    // -------------------------------------------------------
    // Loading state
    // -------------------------------------------------------

    test("shows loading state before data arrives", () => {
        // Never resolves — keeps component in loading state
        eventService.getEvents.mockReturnValue(new Promise(() => {}));
        render(<EventList />);
        expect(screen.getByText(/Loading events/i)).toBeInTheDocument();
    });

    // -------------------------------------------------------
    // Renders events
    // -------------------------------------------------------

    test("renders events after loading", async () => {
        render(<EventList />);
        expect(await screen.findByText("Community Meeting")).toBeInTheDocument();
    });

    // -------------------------------------------------------
    // Empty state
    // -------------------------------------------------------

    test("shows empty state when no events returned", async () => {
        eventService.getEvents.mockResolvedValue(mockPage({ content: [], totalPages: 0 }));
        render(<EventList />);
        expect(await screen.findByText(/No events found/i)).toBeInTheDocument();
    });

    // -------------------------------------------------------
    // Error state
    // -------------------------------------------------------

    test("shows error state on API failure", async () => {
        eventService.getEvents.mockRejectedValue(new Error("Server Error"));
        render(<EventList />);
        expect(await screen.findByText(/Failed to load events/i)).toBeInTheDocument();
    });

    // -------------------------------------------------------
    // Search functionality
    // -------------------------------------------------------

    test("calls searchEvents API when keyword is typed", async () => {
        render(<EventList />);
        await screen.findByText("Community Meeting");

        const input = screen.getByPlaceholderText(/Search events/i);
        await userEvent.clear(input);
        await userEvent.type(input, "meeting");

        expect(input).toHaveValue("meeting");
        await waitFor(() => {
            expect(eventService.searchEvents).toHaveBeenCalled();
        });
    });

    test("calls getEvents (not searchEvents) when keyword is cleared", async () => {
        render(<EventList />);
        await screen.findByText("Community Meeting");

        const input = screen.getByPlaceholderText(/Search events/i);
        await userEvent.type(input, "meeting");
        await waitFor(() => expect(eventService.searchEvents).toHaveBeenCalled());

        jest.clearAllMocks();
        eventService.getEvents.mockResolvedValue(mockPage());
        await userEvent.clear(input);

        await waitFor(() => {
            expect(eventService.getEvents).toHaveBeenCalled();
        });
    });

    // -------------------------------------------------------
    // Pagination buttons exist and are correctly disabled
    // -------------------------------------------------------

    test("Previous button is disabled on first page", async () => {
        render(<EventList />);
        await screen.findByText("Community Meeting");
        expect(screen.getByRole("button", { name: /Previous page/i })).toBeDisabled();
    });

    test("Next button is enabled when more pages exist", async () => {
        render(<EventList />);
        await screen.findByText("Community Meeting");
        expect(screen.getByRole("button", { name: /Next page/i })).not.toBeDisabled();
    });

    test("clicking Next increments page and calls API again", async () => {
        render(<EventList />);
        await screen.findByText("Community Meeting");

        const callsBefore = eventService.getEvents.mock.calls.length;
        await userEvent.click(screen.getByRole("button", { name: /Next page/i }));

        await waitFor(() => {
            expect(eventService.getEvents.mock.calls.length).toBeGreaterThan(callsBefore);
        });

        // Second call should have page: 1
        const lastCall = eventService.getEvents.mock.calls.at(-1)[0];
        expect(lastCall.page).toBe(1);
    });

    // -------------------------------------------------------
    // Category filter
    // -------------------------------------------------------

    test("changing category filter calls getEvents with correct category", async () => {
        render(<EventList />);
        await screen.findByText("Community Meeting");

        const select = screen.getByRole("combobox", { name: /Filter by category/i });
        await userEvent.selectOptions(select, "SOCIAL");

        await waitFor(() => {
            const lastCall = eventService.getEvents.mock.calls.at(-1)[0];
            expect(lastCall.category).toBe("SOCIAL");
            expect(lastCall.page).toBe(0);
        });
    });

    // -------------------------------------------------------
    // Sort direction
    // -------------------------------------------------------

    test("changing sort direction calls getEvents with correct direction", async () => {
        render(<EventList />);
        await screen.findByText("Community Meeting");

        const select = screen.getByRole("combobox", { name: /Sort direction/i });
        await userEvent.selectOptions(select, "desc");

        await waitFor(() => {
            const lastCall = eventService.getEvents.mock.calls.at(-1)[0];
            expect(lastCall.sortDirection).toBe("desc");
        });
    });

    // -------------------------------------------------------
    // View toggle
    // -------------------------------------------------------

    test("clicking List view button switches view", async () => {
        render(<EventList />);
        await screen.findByText("Community Meeting");

        await userEvent.click(screen.getByRole("button", { name: /List view/i }));
        expect(screen.getByRole("button", { name: /List view/i })).toHaveClass("active-view");
    });

});

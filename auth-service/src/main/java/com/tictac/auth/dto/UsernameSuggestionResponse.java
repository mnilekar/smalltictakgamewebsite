package com.tictac.auth.dto;

import java.util.List;

public class UsernameSuggestionResponse {
    private List<String> suggestions;
    public UsernameSuggestionResponse(List<String> suggestions) { this.suggestions = suggestions; }
    public List<String> getSuggestions() { return suggestions; }
}
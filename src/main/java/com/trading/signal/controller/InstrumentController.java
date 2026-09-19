package com.trading.signal.controller;

import com.trading.signal.dto.instrument.InstrumentRequest;
import com.trading.signal.dto.instrument.InstrumentResponse;
import com.trading.signal.entity.Instrument.InstrumentType;
import com.trading.signal.service.InstrumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    public List<InstrumentResponse> list(
            @RequestParam(required = false) InstrumentType type,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        return instrumentService.findAll(type, activeOnly);
    }

    @GetMapping("/{id}")
    public InstrumentResponse get(@PathVariable Long id) {
        return instrumentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstrumentResponse create(@Valid @RequestBody InstrumentRequest request) {
        return instrumentService.create(request);
    }

    @PutMapping("/{id}")
    public InstrumentResponse update(@PathVariable Long id, @Valid @RequestBody InstrumentRequest request) {
        return instrumentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        instrumentService.deactivate(id);
    }
}

package com.trading.signal.service;

import com.trading.signal.dto.instrument.InstrumentRequest;
import com.trading.signal.dto.instrument.InstrumentResponse;
import com.trading.signal.entity.Instrument;
import com.trading.signal.entity.Instrument.InstrumentType;
import com.trading.signal.exception.DuplicateResourceException;
import com.trading.signal.exception.ResourceNotFoundException;
import com.trading.signal.repository.InstrumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    public List<InstrumentResponse> findAll(InstrumentType type, Boolean activeOnly) {
        List<Instrument> instruments;
        if (type != null && Boolean.TRUE.equals(activeOnly)) {
            instruments = instrumentRepository.findByActiveTrueAndType(type);
        } else if (type != null) {
            instruments = instrumentRepository.findByType(type);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            instruments = instrumentRepository.findByActiveTrue();
        } else {
            instruments = instrumentRepository.findAll();
        }
        return instruments.stream().map(this::toResponse).toList();
    }

    public InstrumentResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public InstrumentResponse create(InstrumentRequest request) {
        if (instrumentRepository.existsBySymbol(request.symbol().toUpperCase())) {
            throw new DuplicateResourceException("Symbol already exists: " + request.symbol());
        }
        Instrument entity = Instrument.builder()
                .symbol(request.symbol().toUpperCase())
                .type(request.type())
                .exchange(request.exchange())
                .build();
        return toResponse(instrumentRepository.save(entity));
    }

    public InstrumentResponse update(Long id, InstrumentRequest request) {
        Instrument entity = getOrThrow(id);
        // Symbol change? Check for duplicate
        String newSymbol = request.symbol().toUpperCase();
        if (!entity.getSymbol().equals(newSymbol) && instrumentRepository.existsBySymbol(newSymbol)) {
            throw new DuplicateResourceException("Symbol already exists: " + newSymbol);
        }
        entity.setSymbol(newSymbol);
        entity.setType(request.type());
        entity.setExchange(request.exchange());
        return toResponse(instrumentRepository.save(entity));
    }

    /** Soft delete: is_active = false (per plan, không xóa cứng) */
    public void deactivate(Long id) {
        Instrument entity = getOrThrow(id);
        entity.setActive(false);
        instrumentRepository.save(entity);
    }

    private Instrument getOrThrow(Long id) {
        return instrumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument", id));
    }

    private InstrumentResponse toResponse(Instrument i) {
        return new InstrumentResponse(i.getId(), i.getSymbol(), i.getType().name(),
                i.getExchange(), i.isActive());
    }
}

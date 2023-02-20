/*
 * This file is generated by jOOQ.
 */
package com.zenith.database.dto.tables.records;


import com.zenith.database.dto.tables.Restarts;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Row1;
import org.jooq.impl.TableRecordImpl;

import java.time.OffsetDateTime;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class RestartsRecord extends TableRecordImpl<RestartsRecord> implements Record1<OffsetDateTime> {

    private static final long serialVersionUID = 1L;

    /**
     * Create a detached RestartsRecord
     */
    public RestartsRecord() {
        super(Restarts.RESTARTS);
    }

    /**
     * Create a detached, initialised RestartsRecord
     */
    public RestartsRecord(OffsetDateTime time) {
        super(Restarts.RESTARTS);

        setTime(time);
    }

    // -------------------------------------------------------------------------
    // Record1 type implementation
    // -------------------------------------------------------------------------

    /**
     * Getter for <code>public.restarts.time</code>.
     */
    public OffsetDateTime getTime() {
        return (OffsetDateTime) get(0);
    }

    /**
     * Setter for <code>public.restarts.time</code>.
     */
    public RestartsRecord setTime(OffsetDateTime value) {
        set(0, value);
        return this;
    }

    @Override
    public Row1<OffsetDateTime> fieldsRow() {
        return (Row1) super.fieldsRow();
    }

    @Override
    public Row1<OffsetDateTime> valuesRow() {
        return (Row1) super.valuesRow();
    }

    @Override
    public Field<OffsetDateTime> field1() {
        return Restarts.RESTARTS.TIME;
    }

    @Override
    public OffsetDateTime component1() {
        return getTime();
    }

    @Override
    public OffsetDateTime value1() {
        return getTime();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    @Override
    public RestartsRecord value1(OffsetDateTime value) {
        setTime(value);
        return this;
    }

    @Override
    public RestartsRecord values(OffsetDateTime value1) {
        value1(value1);
        return this;
    }
}
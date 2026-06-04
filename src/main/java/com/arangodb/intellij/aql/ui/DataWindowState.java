package com.arangodb.intellij.aql.ui;

import com.arangodb.intellij.aql.model.ArangoDbServer;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Service(Service.Level.PROJECT)
@State(name = "ArangoDB.DataSource", storages = {@Storage("ArangoDB_DataSource.xml")})
public class DataWindowState implements PersistentStateComponent<ArangoDbServer> {

    private boolean processed;
    private ArangoDbServer state;

    @Override
    public void loadState(@NotNull ArangoDbServer state) {
        this.state = state;
    }

    @NotNull
    @Override
    public ArangoDbServer getState() {
        if (state == null) {
            state = new ArangoDbServer();
        }
        return state;
    }

    @Transient
    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(final boolean processed) {
        this.processed = processed;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DataWindowState that = (DataWindowState) o;
        return Objects.equals(getState(), that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getState());
    }
}

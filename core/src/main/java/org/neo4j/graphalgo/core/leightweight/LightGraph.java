package org.neo4j.graphalgo.core.leightweight;

import org.neo4j.graphalgo.core.IdMap;
import org.neo4j.graphalgo.core.WeightMap;
import org.neo4j.collection.primitive.PrimitiveIntIterator;
import org.neo4j.graphalgo.api.*;
import org.neo4j.graphdb.Direction;

import java.util.Iterator;
import java.util.function.IntConsumer;

/**
 *
 * @author phorn@avantgarde-labs.de
 */
public class LightGraph implements Graph {

    private final IdMap idMapping;
    private final WeightMapping weightMapping;
    private final IntArray adjacency;
    private final long[] inOffsets;
    private final long[] outOffsets;
    private final IntArray.Cursor spare;

    LightGraph(
            final IdMap idMapping,
            final WeightMapping weightMapping,
            final IntArray adjacency,
            final long[] inOffsets,
            final long[] outOffsets) {
        this.idMapping = idMapping;
        this.weightMapping = weightMapping;
        this.adjacency = adjacency;
        this.inOffsets = inOffsets;
        this.outOffsets = outOffsets;
        this.spare = adjacency.newCursor();
    }

    @Override
    public int nodeCount() {
        return idMapping.size();
    }

    @Override
    public PrimitiveIntIterator nodeIterator() {
        return idMapping.iterator();
    }

    @Override
    public void forEachNode(IntConsumer consumer) {
        idMapping.forEach(consumer);
    }

    @Override
    public void forEachRelation(int vertexId, Direction direction, RelationConsumer consumer) {
        switch (direction) {
            case INCOMING:
                forEachIncoming(vertexId, consumer);
                return;

            case OUTGOING:
                forEachOutgoing(vertexId, consumer);
                return;

            case BOTH:
                forEachIncoming(vertexId, consumer);
                forEachOutgoing(vertexId, consumer);
                return;

            default:
                throw new IllegalArgumentException(direction + "");
        }
    }

    @Override
    public void forEachRelation(int vertexId, Direction direction, WeightedRelationConsumer consumer) {
        switch (direction) {
            case INCOMING:
                forEachIncoming(vertexId, consumer);
                return;

            case OUTGOING:
                forEachOutgoing(vertexId, consumer);
                return;

            case BOTH:
                forEachIncoming(vertexId, consumer);
                forEachOutgoing(vertexId, consumer);
                return;

            default:
                throw new IllegalArgumentException(direction + "");
        }
    }

    @Override
    public Iterator<WeightedRelationCursor> weightedRelationIterator(int vertexId, Direction direction) {

        switch (direction) {
            case INCOMING: {
                final long offset = inOffsets[vertexId];
                final int length = adjacency.get(offset);
                return new WeightedRelationIteratorImpl(vertexId, offset + 1, offset + length, weightMapping, adjacency);
            }

            case OUTGOING: {
                final long offset = outOffsets[vertexId];
                final int length = adjacency.get(offset);
                return new WeightedRelationIteratorImpl(vertexId, offset + 1, offset + length, weightMapping, adjacency);
            }
            default: {
                throw new IllegalArgumentException("Direction.BOTH not yet implemented");
            }
        }
    }

    @Override
    public Iterator<RelationCursor> relationIterator(int vertexId, Direction direction) {

        switch (direction) {
            case INCOMING: {
                final long offset = inOffsets[vertexId];
                final int length = adjacency.get(offset);
                return new RelationIteratorImpl(vertexId, offset + 1, length, adjacency);
            }

            case OUTGOING: {
                final long offset = outOffsets[vertexId];
                final int length = adjacency.get(offset);
                return new RelationIteratorImpl(vertexId, offset + 1, length, adjacency);
            }

            default: {
                throw new IllegalArgumentException("Not yet implemented");
            }

        }
    }

    @Override
    public int degree(
            final int node,
            final Direction direction) {
        switch (direction) {
            case INCOMING:
                return adjacency.get(inOffsets[node]);

            case OUTGOING:
                return adjacency.get(outOffsets[node]);

            case BOTH:
                return adjacency.get(inOffsets[node])
                        + adjacency.get(outOffsets[node]);

            default:
                throw new IllegalArgumentException(direction + "");
        }

    }

    @Override
    public int toMappedNodeId(long nodeId) {
        return idMapping.get(nodeId);
    }

    @Override
    public long toOriginalNodeId(int vertexId) {
        return idMapping.unmap(vertexId);
    }

    private void forEachIncoming(
            final int node,
            final RelationConsumer consumer) {
        consumeNodes(node, cursor(node, inOffsets), consumer);
    }

    private void forEachOutgoing(
            final int node,
            final RelationConsumer consumer) {
        consumeNodes(node, cursor(node, outOffsets), consumer);
    }

    private void forEachIncoming(
            final int node,
            final WeightedRelationConsumer consumer) {
        consumeNodes(node, cursor(node, inOffsets), consumer);
    }

    private void forEachOutgoing(
            final int node,
            final WeightedRelationConsumer consumer) {
        consumeNodes(node, cursor(node, outOffsets), consumer);
    }

    private IntArray.Cursor cursor(int node, long[] offsets) {
        final long offset = offsets[node];
        final int length = adjacency.get(offset);
        return adjacency.cursor(offset + 1, length, spare);
    }

    private void consumeNodes(
            int node,
            IntArray.Cursor cursor,
            WeightedRelationConsumer consumer) {
        while (cursor.next()) {
            final int[] array = cursor.array;
            int offset = cursor.offset;
            final int limit = cursor.length + offset;
            while (offset < limit) {
                consumer.accept(node, array[offset], offset, weightMapping.get(offset++));
            }
        }
    }

    private void consumeNodes(
            int node,
            IntArray.Cursor cursor,
            RelationConsumer consumer) {
        while (cursor.next()) {
            final int[] array = cursor.array;
            int offset = cursor.offset;
            final int limit = cursor.length + offset;
            while (offset < limit) {
                consumer.accept(node, array[offset], offset++);
            }
        }
    }
}
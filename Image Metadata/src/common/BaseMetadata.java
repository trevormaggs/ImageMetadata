package common;

/**
 * A marker interface that enables multiple metadata sub-types to share a common super-type.
 * 
 * This allows for polymorphic handling of different metadata implementations without enforcing
 * method definitions at this level. It supports the composite design pattern by enabling generic
 * metadata containers to manage heterogeneous metadata components.
 * 
 * @see Metadata
 */
public interface BaseMetadata
{
}
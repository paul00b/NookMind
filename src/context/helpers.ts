export function prependItem<T>(items: T[], item: T) {
  return [item, ...items];
}

export function replaceItemById<T extends { id: string }>(items: T[], item: T) {
  return items.map((current) => (current.id === item.id ? item : current));
}

export function removeItemById<T extends { id: string }>(items: T[], id: string) {
  return items.filter((item) => item.id !== id);
}

export function mergeUniqueIds(existingIds: string[], nextIds: string[]) {
  return [...new Set([...existingIds, ...nextIds])];
}

export function removeMappedId<T>(
  items: T[],
  targetId: string,
  getItemId: (item: T) => string,
  updateIds: (item: T, nextIds: string[]) => T,
  mappedId: string,
  getMappedIds: (item: T) => string[],
) {
  return items.map((item) =>
    getItemId(item) === targetId
      ? updateIds(
          item,
          getMappedIds(item).filter((id) => id !== mappedId),
        )
      : item,
  );
}

export function addMappedIds<T>(
  items: T[],
  targetId: string,
  getItemId: (item: T) => string,
  updateIds: (item: T, nextIds: string[]) => T,
  mappedIds: string[],
  getMappedIds: (item: T) => string[],
) {
  return items.map((item) =>
    getItemId(item) === targetId
      ? updateIds(item, mergeUniqueIds(getMappedIds(item), mappedIds))
      : item,
  );
}
